package com.scraper.leadscraper.billing;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class BillingTest {
 @TempDir Path folder;
 Billing billing(String key){return new Billing(new MockEnvironment().withProperty("heimdall.billing.directory",folder.toString()).withProperty("RAZORPAY_KEY_ID",key).withProperty("RAZORPAY_KEY_SECRET","test-secret"),new ObjectMapper());}
 @Test void unpaidAccountsAreLockedAndTestPurchasesCannotUnlockLive()throws Exception{assertFalse(billing("rzp_test_demo").premium("a@example.com"));Files.writeString(folder.resolve("test.properties"),"premium.a@example.com=pay_test\n");assertTrue(billing("rzp_test_demo").premium("a@example.com"));assertFalse(billing("rzp_live_demo").premium("a@example.com"));}
 @Test void foreignOrdersAndInvalidSignaturesNeverGrantAccess()throws Exception{Files.writeString(folder.resolve("test.properties"),"order.order_demo=a@example.com\n");assertThrows(ResponseStatusException.class,()->billing("rzp_test_demo").verify("b@example.com","order_demo","pay_demo","bad"));assertThrows(ResponseStatusException.class,()->billing("rzp_test_demo").verify("a@example.com","order_demo","pay_demo","bad"));assertFalse(billing("rzp_test_demo").premium("a@example.com"));}
 @Test void missingKeysCannotCreatePurchase() {var b=new Billing(new MockEnvironment().withProperty("heimdall.billing.directory",folder.toString()),new ObjectMapper());assertFalse(b.configured());assertThrows(ResponseStatusException.class,()->b.order("a@example.com"));}
 @Test void operatorGrantIsExactAndIndependentOfPaymentMode(){
  var env=new MockEnvironment().withProperty("heimdall.billing.directory",folder.toString()).withProperty("HEIMDALL_PREMIUM_EMAILS","Owner@Example.com");
  var b=new Billing(env,new ObjectMapper());assertTrue(b.premium("owner@example.com"));assertFalse(b.premium("not-owner@example.com"));assertFalse(b.premium("owner@example.com.evil"));
 }
 String signature(String body,String secret)throws Exception{
  var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));return java.util.HexFormat.of().formatHex(mac.doFinal(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
 }
 @Test void signedWebhooksGrantOnceAndRefundedPaymentsCannotBeReplayed()throws Exception{
  var env=new MockEnvironment().withProperty("heimdall.billing.directory",folder.toString()).withProperty("RAZORPAY_KEY_ID","rzp_live_demo").withProperty("RAZORPAY_KEY_SECRET","api-secret").withProperty("RAZORPAY_WEBHOOK_SECRET","hook-secret");
  String[] state={"captured"};int[] refunded={0};
  var b=new Billing(env,new ObjectMapper()){
   @Override tools.jackson.databind.JsonNode request(String path,java.util.Map<String,Object> body)throws Exception{return new ObjectMapper().readTree("{\"id\":\"pay_demo\",\"order_id\":\"order_demo\",\"status\":\""+state[0]+"\",\"amount\":10000,\"currency\":\"INR\",\"amount_refunded\":"+refunded[0]+"}");}
  };
  Files.writeString(folder.resolve("live.properties"),"order.order_demo=a@example.com\npending.a@example.com=order_demo\n");
  String captured="{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_demo\"}}}}";
  assertThrows(ResponseStatusException.class,()->b.webhook(captured,"bad"));assertFalse(b.premium("a@example.com"));
  b.webhook(captured,signature(captured,"hook-secret"));b.webhook(captured,signature(captured,"hook-secret"));assertTrue(b.premium("a@example.com"));
  String refund="{\"event\":\"refund.processed\",\"payload\":{\"refund\":{\"entity\":{\"payment_id\":\"pay_demo\"}}}}";
  state[0]="refunded";refunded[0]=10000;b.webhook(refund,signature(refund,"hook-secret"));assertFalse(b.premium("a@example.com"));
  b.webhook(captured,signature(captured,"hook-secret"));assertFalse(b.premium("a@example.com"));
 }
 @Test void missingWebhookSecretRejectsNotifications(){assertThrows(ResponseStatusException.class,()->billing("rzp_live_demo").webhook("{}","bad"));}
}
