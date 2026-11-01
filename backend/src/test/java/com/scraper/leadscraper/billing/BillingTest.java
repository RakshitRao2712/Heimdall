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
}
