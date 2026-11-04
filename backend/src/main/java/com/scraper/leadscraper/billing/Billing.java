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

    @Autowired(required = false)
    void usePostgres(PostgresDocuments documents) {
        this.documents = documents;
    }

 private final String key,secret; private final Path file; private final ObjectMapper json;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(15)).build();
 public Billing(Environment env,ObjectMapper json){this.json=json;key=env.getProperty("RAZORPAY_KEY_ID","").trim();secret=env.getProperty("RAZORPAY_KEY_SECRET","").trim();file=Path.of(env.getProperty("heimdall.billing.directory",".data/billing"),key.startsWith("rzp_live_")?"live.properties":"test.properties").toAbsolutePath();}
 public boolean testMode(){return !key.startsWith("rzp_live_");}
 public boolean configured(){return !key.isEmpty()&&!secret.isEmpty();}
 private Properties read() throws Exception{if (documents != null) return documents.readProperties("billing", file.getFileName().toString()); Properties p=new Properties();if(Files.exists(file))try(var in=Files.newInputStream(file)){p.load(in);}return p;}
 private void save(Properties p)throws Exception{
  if (documents != null) {
   documents.writeProperties("billing", file.getFileName().toString(), p);
   return;
  }
  Files.createDirectories(file.getParent());Path t=Files.createTempFile(file.getParent(),"billing-",".tmp");try{try{Files.setPosixFilePermissions(t,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));}catch(UnsupportedOperationException ignored){}try(var out=Files.newOutputStream(t)){p.store(out,"Verified Heimdall purchases");}Files.move(t,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(t);}}
 public synchronized boolean premium(String email){try{return read().containsKey("premium."+email);}catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cannot check purchase status");}}
 private JsonNode request(String path,Map<String,Object> body)throws Exception{if(!configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Purchases are not configured yet.");var b=HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1/"+path)).timeout(java.time.Duration.ofSeconds(25)).header("Authorization","Basic "+Base64.getEncoder().encodeToString((key+":"+secret).getBytes(StandardCharsets.UTF_8)));if(body!=null)b.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));var r=http.send(b.build(),HttpResponse.BodyHandlers.ofString());if(r.statusCode()/100!=2)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Payment provider could not complete this request. Please retry.");return json.readTree(r.body());}
 public synchronized Map<String,Object> order(String email)throws Exception{if(premium(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"You already have Premium.");Properties p=read();String existing=p.getProperty("pending."+email);JsonNode o=existing==null?request("orders",Map.of("amount",10000,"currency","INR","receipt",UUID.randomUUID().toString())):request("orders/"+existing,null);String id=o.path("id").asText();if(!id.matches("order_[A-Za-z0-9]+"))throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Invalid payment order");p.setProperty("order."+id,email);p.setProperty("pending."+email,id);save(p);return Map.of("key",key,"orderId",id,"amount",10000,"currency","INR");}
 public synchronized void verify(String email,String order,String payment,String signature)throws Exception{Properties p=read();if(order==null||payment==null||signature==null||!email.equals(p.getProperty("order."+order))||!payment.matches("pay_[A-Za-z0-9]+"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid purchase details");Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));String expected=HexFormat.of().formatHex(mac.doFinal((order+"|"+payment).getBytes(StandardCharsets.UTF_8)));if(!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),signature.getBytes(StandardCharsets.UTF_8)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Payment verification failed");JsonNode paid=request("payments/"+payment,null);if(!order.equals(paid.path("order_id").asText())||!"captured".equals(paid.path("status").asText())||paid.path("amount").asInt()!=10000||!"INR".equals(paid.path("currency").asText())||paid.path("amount_refunded").asInt()!=0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment is not captured yet. Retry verification shortly.");p.setProperty("premium."+email,payment);p.remove("pending."+email);save(p);}
}
