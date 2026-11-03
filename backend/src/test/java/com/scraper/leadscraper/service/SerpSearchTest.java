package com.scraper.leadscraper.service;

import com.scraper.leadscraper.auth.SerpKeyController;
import com.scraper.leadscraper.auth.BraveKeyController;
import com.scraper.leadscraper.dto.GoogleSearchLead;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;
import java.net.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SerpSearchTest {
 @Test void keyIsSessionScopedAndIndependentOfBrave() {
  var controller=new SerpKeyController();var a=new MockHttpSession();var b=new MockHttpSession();
  assertThrows(ResponseStatusException.class,()->controller.save(new SerpKeyController.KeyInput("invalid\nkey"),a));
  controller.save(new SerpKeyController.KeyInput("test-serp-key-12345678901234567890"),a);
  assertFalse(controller.status(b).get("configured"));assertThrows(ResponseStatusException.class,()->SerpKeyController.requireKey(b));
  assertThrows(ResponseStatusException.class,()->BraveKeyController.requireKey(a));
  controller.remove(a);assertThrows(ResponseStatusException.class,()->SerpKeyController.requireKey(a));
 }
 @Test void parsesOrganicResultsAndPaginationWithoutFollowingExternalUrls() {
  var page=GoogleSearchScraperService.parseSerpPage("""
   {"organic_results":[{"title":"Cafe","link":"https://cafe.example/contact","snippet":"Copyright 2024"},
   {"link":"javascript:alert(1)"},{"link":"https://google.com/search?q=test"}],
   "serpapi_pagination":{"next":"https://untrusted.example/next"}}
   """);
  assertEquals(1,page.results().size());assertTrue(page.more());assertEquals("Cafe",page.results().getFirst().title());
  var uri=GoogleSearchScraperService.serpUri("cafes in London",2,"private-key");
  assertEquals("serpapi.com",uri.getHost());assertTrue(uri.getRawQuery().contains("start=20"));assertTrue(uri.getRawQuery().contains("engine=google"));
  assertFalse(GoogleSearchScraperService.parseSerpPage("{\"organic_results\":[]}").more());
  assertTrue(GoogleSearchScraperService.parseSerpPage("{\"error\":\"Google hasn't returned any results for this query.\"}").results().isEmpty());
  var error=assertThrows(ResponseStatusException.class,()->GoogleSearchScraperService.parseSerpPage("{\"error\":\"private-key invalid\"}"));assertFalse(error.getReason().contains("private-key"));
 }
 @Test @SuppressWarnings("unchecked") void providerUsesPersonalKeyAndDoesNotExposeProviderErrors() throws Exception {
  var service=new GoogleSearchScraperService(20,1,2);
  try {
   var client=mock(HttpClient.class);var response=mock(HttpResponse.class);
   var field=GoogleSearchScraperService.class.getDeclaredField("client");field.setAccessible(true);((HttpClient)field.get(service)).close();field.set(service,client);
   when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
   when(response.statusCode()).thenReturn(200);when(response.body()).thenReturn("{\"organic_results\":[]}");
   service.serpPage("cafes",0,"alice-secret");
   var request=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);verify(client).send(request.capture(),any(HttpResponse.BodyHandler.class));assertTrue(request.getValue().uri().getRawQuery().contains("api_key=alice-secret"));
   for(int status:new int[]{401,403,429,500}) {when(response.statusCode()).thenReturn(status);var error=assertThrows(ResponseStatusException.class,()->service.serpPage("cafes",0,"bob-secret"));assertFalse(error.getReason().contains("bob-secret"));}
   assertThrows(ResponseStatusException.class,()->service.serpPage("cafes",0,""));
  } finally {service.close();}
 }
 @Test void targetDeduplicatesAndCancellationPreventsCalls() {
  var calls=new ArrayList<Integer>();
  var service=new GoogleSearchScraperService(20,1,4){
   @Override protected BravePage serpPage(String q,int page,String key) {
    calls.add(page);var items=new ArrayList<GoogleSearchResult>();for(int i=page*8;i<page*8+10;i++)items.add(new GoogleSearchResult("https://b"+i+".example","Business",""));return new BravePage(items,true);
   }
   @Override protected BravePage bravePage(String q,int page,String key){throw new AssertionError("Must use SerpApi");}
   @Override GoogleSearchLead processWebsite(GoogleSearchResult r){return new GoogleSearchLead(r.url(),r.title(),List.of("1234567890"),"2024","");}
  };
  try {
   var stopped=service.searchSerpUntilTarget("cafes",3,"key",25,2023,2024,()->true,p->{});assertTrue(calls.isEmpty());assertTrue(stopped.progress().reason().contains("Cancelled"));
   var result=service.searchSerpUntilTarget("cafes",3,"key",25,2023,2024,()->false,p->{});
   assertEquals(25,result.leads().size());assertEquals(3,result.progress().requests());assertEquals(List.of(0,1,2),calls);assertEquals("Target reached",result.progress().reason());assertEquals(25,result.leads().stream().map(GoogleSearchLead::websiteUrl).distinct().count());
  } finally {service.close();}
 }
}
