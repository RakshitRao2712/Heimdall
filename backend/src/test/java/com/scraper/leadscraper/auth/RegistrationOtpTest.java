package com.scraper.leadscraper.auth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class RegistrationOtpTest {
 @TempDir Path directory;
 static class TestClock extends Clock {
  long now=1_800_000_000_000L;
  public ZoneId getZone(){return ZoneOffset.UTC;}
  public Clock withZone(ZoneId zone){return this;}
  public Instant instant(){return Instant.ofEpochMilli(now);}
 }
 @Test void accountOnlyCreatedAfterVerificationAndCodeIsSingleUse() throws Exception {
  var accounts=new AccountStore(directory.resolve("accounts").toString());String[] code={null};
  var service=new RegistrationOtp(accounts,(email,value)->code[0]=value,new TestClock());
  var auth=new AuthController(accounts,new BCryptPasswordEncoder(4),new HttpSessionSecurityContextRepository(),service);
  var request=new MockHttpServletRequest();request.setRemoteAddr("127.0.0.1");
  var challenge=auth.register(new AuthController.Credentials("test@example.com","Test-password-2026!",false),request);
  assertNull(accounts.hash("test@example.com"));assertNull(request.getSession(false));assertTrue(code[0].matches("[0-9]{6}"));
  String wrong=code[0].equals("000000")?"000001":"000000";
  assertThrows(ResponseStatusException.class,()->service.verify(challenge.challengeId(),wrong));assertNull(accounts.hash("test@example.com"));
  assertEquals("test@example.com",auth.verify(new AuthController.OtpInput(challenge.challengeId(),code[0]),request,new MockHttpServletResponse()).get("email"));
  assertNotNull(accounts.hash("test@example.com"));assertNotNull(request.getSession(false));
  assertThrows(ResponseStatusException.class,()->service.verify(challenge.challengeId(),code[0]));
 }
 @Test void expiryAndAttemptLimitPreventAccountCreation() throws Exception {
  var accounts=new AccountStore(directory.resolve("accounts").toString());var clock=new TestClock();String[] code={null};
  var service=new RegistrationOtp(accounts,(email,value)->code[0]=value,clock);
  var challenge=service.start("expired@example.com","hash",false);clock.now+=600_000;
  assertThrows(ResponseStatusException.class,()->service.verify(challenge.challengeId(),code[0]));
  var other=service.start("guess@example.com","hash",false);
  for(int i=0;i<5;i++)assertThrows(ResponseStatusException.class,()->service.verify(other.challengeId(),"invalid"));
  assertThrows(ResponseStatusException.class,()->service.verify(other.challengeId(),code[0]));
  assertNull(accounts.hash("expired@example.com"));assertNull(accounts.hash("guess@example.com"));
 }
 @Test void resendReplacesOldChallengeAndLimitsEmailVolume() throws Exception {
  var accounts=new AccountStore(directory.resolve("accounts").toString());var clock=new TestClock();String[] code={null};
  var service=new RegistrationOtp(accounts,(email,value)->code[0]=value,clock);
  var challenge=service.start("resend@example.com","hash",true);
  assertEquals(429,assertThrows(ResponseStatusException.class,()->service.resend(challenge.challengeId())).getStatusCode().value());
  clock.now+=60_000;var replacement=service.resend(challenge.challengeId());
  assertThrows(ResponseStatusException.class,()->service.verify(challenge.challengeId(),code[0]));
  assertTrue(service.verify(replacement.challengeId(),code[0]).rememberMe());
  for(int i=0;i<5;i++){clock.now+=60_000;service.start("limit@example.com","hash",false);}
  clock.now+=60_000;
  assertEquals(429,assertThrows(ResponseStatusException.class,()->service.start("limit@example.com","hash",false)).getStatusCode().value());
 }
 @Test void missingEmailConfigurationFailsClosed() throws Exception {
  var accounts=new AccountStore(directory.resolve("accounts").toString());
  var service=new RegistrationOtp(accounts,new ResendOtpMailer("","",new ObjectMapper()),new TestClock());
  assertEquals(503,assertThrows(ResponseStatusException.class,()->service.start("no-mail@example.com","hash",false)).getStatusCode().value());
  assertNull(accounts.hash("no-mail@example.com"));
 }
}
