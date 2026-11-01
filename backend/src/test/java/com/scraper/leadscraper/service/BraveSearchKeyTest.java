package com.scraper.leadscraper.service;
import com.scraper.leadscraper.auth.BraveKeyController;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class BraveSearchKeyTest {
 @Test void sessionIsolationReplacementRemovalAndValidation() {
  var c=new BraveKeyController();var a=new MockHttpSession();var b=new MockHttpSession();
  assertThrows(ResponseStatusException.class,()->c.save(new BraveKeyController.KeyInput("bad\nkey"),a));
  assertEquals(java.util.Map.of("configured",true),c.save(new BraveKeyController.KeyInput("test-key-12345678901234567890"),a));
  assertFalse(c.status(b).get("configured"));assertThrows(ResponseStatusException.class,()->BraveKeyController.requireKey(b));
  c.save(new BraveKeyController.KeyInput("replacement-12345678901234567890"),a);assertEquals("replacement-12345678901234567890",BraveKeyController.requireKey(a));
  c.remove(a);assertThrows(ResponseStatusException.class,()->BraveKeyController.requireKey(a));
 }
 @Test @SuppressWarnings("unchecked") void requestScopedKeyAndSafeProviderErrors() throws Exception {
  var s=new GoogleSearchScraperService(20,10,2); try {
   var client=mock(HttpClient.class);var response=mock(HttpResponse.class);
   var f=GoogleSearchScraperService.class.getDeclaredField("client");f.setAccessible(true);((HttpClient)f.get(s)).close();f.set(s,client);
   when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
   when(response.statusCode()).thenReturn(200);when(response.body()).thenReturn("{\"web\":{\"results\":[]}}");
   assertTrue(s.searchAndExtractLeads("cafes",1,"alice-secret").isEmpty());
   var capture=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);verify(client).send(capture.capture(),any(HttpResponse.BodyHandler.class));assertEquals("alice-secret",capture.getValue().headers().firstValue("X-Subscription-Token").orElseThrow());
   for(int status:new int[]{401,403,429,500}) {when(response.statusCode()).thenReturn(status);var e=assertThrows(ResponseStatusException.class,()->s.searchAndExtractLeads("cafes",1,"bob-secret"));assertFalse(e.getReason().contains("bob-secret"));}
   assertThrows(ResponseStatusException.class,()->s.searchAndExtractLeads("cafes",1,""));
  } finally { s.close(); }
 }
}
