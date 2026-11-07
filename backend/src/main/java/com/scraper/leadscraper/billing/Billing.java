package com.scraper.leadscraper.billing;

import com.scraper.leadscraper.storage.PostgresDocuments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

@Service
public class Billing {
 private PostgresDocuments documents;
 @Autowired(required=false) void usePostgres(PostgresDocuments documents){this.documents=documents;}
 private final String key,secret,webhookSecret;
 private final Set<String> grantedEmails;
 private final Path file;
 private final ObjectMapper json;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(15)).build();
 public Billing(Environment env,ObjectMapper json){
  this.json=json;
  key=env.getProperty("RAZORPAY_KEY_ID","").trim();
  secret=env.getProperty("RAZORPAY_KEY_SECRET","").trim();
  webhookSecret=env.getProperty("RAZORPAY_WEBHOOK_SECRET","").trim();
  grantedEmails=new HashSet<>();
  for(String email:env.getProperty("HEIMDALL_PREMIUM_EMAILS","").split(","))if(!email.isBlank())grantedEmails.add(email.trim().toLowerCase(Locale.ROOT));
  file=Path.of(env.getProperty("heimdall.billing.directory",".data/billing"),key.startsWith("rzp_live_")?"live.properties":"test.properties").toAbsolutePath();
 }
 public boolean testMode(){return !key.startsWith("rzp_live_");}
 public boolean configured(){return (key.startsWith("rzp_live_")||key.startsWith("rzp_test_"))&&!secret.isEmpty();}
 private Properties read()throws Exception{if(documents!=null)return documents.readProperties("billing",file.getFileName().toString());Properties p=new Properties();if(Files.exists(file))try(var in=Files.newInputStream(file)){p.load(in);}return p;}
 private void save(Properties p)throws Exception{
  if(documents!=null){documents.writeProperties("billing",file.getFileName().toString(),p);return;}
  Files.createDirectories(file.getParent());Path t=Files.createTempFile(file.getParent(),"billing-",".tmp");
  try{try{Files.setPosixFilePermissions(t,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));}catch(UnsupportedOperationException ignored){}
   try(var out=Files.newOutputStream(t)){p.store(out,"Verified Heimdall purchases");}
   Files.move(t,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
  }finally{Files.deleteIfExists(t);}
 }
 public synchronized boolean premium(String email){
  if(email!=null&&grantedEmails.contains(email.toLowerCase(Locale.ROOT)))return true;
  try{return read().containsKey("premium."+email);}catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cannot check purchase status");}
 }
 JsonNode request(String path,Map<String,Object> body)throws Exception{
  if(!configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Purchases are not configured yet.");
  var b=HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1/"+path)).timeout(java.time.Duration.ofSeconds(25)).header("Authorization","Basic "+Base64.getEncoder().encodeToString((key+":"+secret).getBytes(StandardCharsets.UTF_8)));
  if(body!=null)b.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
  var r=http.send(b.build(),HttpResponse.BodyHandlers.ofString());
  if(r.statusCode()/100!=2)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Payment provider could not complete this request. Please retry.");
  return json.readTree(r.body());
 }
 public synchronized Map<String,Object> order(String email)throws Exception{
  if(premium(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"You already have Premium.");
  Properties p=read();String existing=p.getProperty("pending."+email);
  JsonNode o=existing==null?null:request("orders/"+existing,null);
  if(o==null||!Set.of("created","attempted").contains(o.path("status").asText()))o=request("orders",Map.of("amount",10000,"currency","INR","receipt",UUID.randomUUID().toString()));
  String id=o.path("id").asText();
  if(!id.matches("order_[A-Za-z0-9]+")||o.path("amount").asInt()!=10000||!"INR".equals(o.path("currency").asText()))throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Invalid payment order");
  p.setProperty("order."+id,email);p.setProperty("pending."+email,id);save(p);
  return Map.of("key",key,"orderId",id,"amount",10000,"currency","INR");
 }
 private boolean validSignature(String message,String signature,String signingSecret)throws Exception{
  if(signingSecret.isBlank()||signature==null||!signature.matches("[a-fA-F0-9]{64}"))return false;
  Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
  return MessageDigest.isEqual(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)),HexFormat.of().parseHex(signature));
 }
 private void grantCaptured(Properties p,String email,String order,String payment)throws Exception{
  JsonNode paid=request("payments/"+payment,null);
  if(!order.equals(paid.path("order_id").asText())||!"captured".equals(paid.path("status").asText())||paid.path("amount").asInt()!=10000||!"INR".equals(paid.path("currency").asText())||paid.path("amount_refunded").asInt()!=0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment is not captured or has been refunded. Please contact support.");
  p.setProperty("premium."+email,payment);
  if(order.equals(p.getProperty("pending."+email)))p.remove("pending."+email);
  save(p);
 }
 public synchronized void verify(String email,String order,String payment,String signature)throws Exception{
  Properties p=read();
  if(order==null||payment==null||!email.equals(p.getProperty("order."+order))||!payment.matches("pay_[A-Za-z0-9]+"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid purchase details");
  if(!validSignature(order+"|"+payment,signature,secret))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Payment verification failed");
  grantCaptured(p,email,order,payment);
 }
 public synchronized void webhook(String body,String signature)throws Exception{
  if(webhookSecret.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Payment notifications are not configured.");
  if(!validSignature(body,signature,webhookSecret))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid payment notification signature");
  JsonNode event;
  try{event=json.readTree(body);}catch(Exception invalid){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid payment notification");}
  String type=event.path("event").asText();
  if(!Set.of("payment.captured","order.paid","refund.processed").contains(type))return;
  String payment=type.equals("refund.processed")?event.path("payload").path("refund").path("entity").path("payment_id").asText():event.path("payload").path("payment").path("entity").path("id").asText();
  if(!payment.matches("pay_[A-Za-z0-9]+"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid payment notification");
  // Fetch authoritative state, so duplicate/out-of-order events cannot restore refunded access.
  JsonNode paid=request("payments/"+payment,null);String order=paid.path("order_id").asText();
  Properties p=read();String email=p.getProperty("order."+order);if(email==null)return;
  if(paid.path("amount_refunded").asInt()>0||"refunded".equals(paid.path("status").asText())){
   if(payment.equals(p.getProperty("premium."+email))){p.remove("premium."+email);save(p);}return;
  }
  if(!type.equals("refund.processed"))grantCaptured(p,email,order,payment);
 }
}
