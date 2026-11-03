package com.scraper.leadscraper.service;
import com.scraper.leadscraper.dto.GoogleSearchLead;
import org.junit.jupiter.api.Test;
import org.jsoup.Jsoup;
import tools.jackson.databind.ObjectMapper;
import java.net.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SearchTargetsTest {
 @SuppressWarnings("unchecked") HttpResponse<String> response(String body){var r=mock(HttpResponse.class);when(r.statusCode()).thenReturn(200);when(r.body()).thenReturn(body);return r;}
 String places(int from,int count,String token){var items=new ArrayList<String>();for(int i=from;i<from+count;i++)items.add("{\"id\":\"p"+i+"\",\"businessStatus\":\"OPERATIONAL\",\"displayName\":{\"text\":\"Business "+i+"\"}}");return "{\"places\":["+String.join(",",items)+"],\"nextPageToken\":\""+token+"\"}";}
 @Test void mapsPaginatesDeduplicatesAndStopsAtFifty()throws Exception {
  var c=mock(HttpClient.class);doReturn(response(places(0,20,"a")),response(places(15,20,"b")),response(places(35,20,""))).when(c).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
  var s=new ScraperService(new ObjectMapper(),c);var result=s.search("barbers","key",50,p->{});
  assertEquals(50,result.leads().size());assertEquals(3,result.progress().requests());assertEquals("Target reached",result.progress().reason());
  var capture=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);verify(c,times(3)).send(capture.capture(),any(HttpResponse.BodyHandler.class));assertTrue(capture.getValue().headers().firstValue("X-Goog-FieldMask").orElseThrow().contains("nextPageToken"));
 }
 @Test void mapsEarlyStopAndHonestShortfall()throws Exception {
  var c=mock(HttpClient.class);doReturn(response(places(0,20,"next"))).when(c).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
  var s=new ScraperService(new ObjectMapper(),c);assertEquals(10,s.search("q","key",10,p->{}).leads().size());verify(c,times(1)).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
  reset(c);doReturn(response(places(0,4,""))).when(c).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
  var result=s.search("q","key",50,p->{});assertEquals(4,result.leads().size());assertEquals("Provider results exhausted",result.progress().reason());
 }
 @Test void copyrightCanReturnFiftyAndDeduplicateAcrossPages() {
  var s=new GoogleSearchScraperService(20,1,4){
   @Override protected BravePage bravePage(String q,int page,String key){var items=new ArrayList<GoogleSearchResult>();for(int i=page*20;i<page*20+20;i++)items.add(new GoogleSearchResult("https://business"+i+".example","Business",""));items.add(new GoogleSearchResult("https://business0.example/contact","Duplicate",""));return new BravePage(items,true);}
   @Override GoogleSearchLead processWebsite(GoogleSearchResult r){return new GoogleSearchLead(r.url(),"Business",List.of("1234567890"),"2024","");}
  };
  try{var result=s.search("plumbers",5,"key",50,2023,2024,p->{});assertEquals(50,result.leads().size());assertEquals(3,result.progress().requests());assertEquals(60,result.progress().checked());}finally{s.close();}
 }
 @Test void customYearsAndInvalidRange(){var doc=Jsoup.parse("<footer>© 2010–2025 Business</footer>");assertEquals("2025",GoogleSearchScraperService.findCopyrightYear(doc,2000,2025));assertNull(GoogleSearchScraperService.findCopyrightYear(doc,2023,2024));var s=new GoogleSearchScraperService(20,1,1);try{assertThrows(org.springframework.web.server.ResponseStatusException.class,()->s.search("q",10,"key",50,2025,2020,p->{}));}finally{s.close();}}
 @Test void partialMapsResultsSurviveLaterQuotaFailure()throws Exception {
  var c=mock(HttpClient.class);var quota=mock(HttpResponse.class);when(quota.statusCode()).thenReturn(429);
  doReturn(response(places(0,4,"next")),quota).when(c).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
  var result=new ScraperService(new ObjectMapper(),c).search("q","key",50,p->{});assertEquals(4,result.leads().size());assertEquals(2,result.progress().requests());assertTrue(result.progress().reason().contains("partial"));
 }
 @Test void copyrightHonorsRequestCapWithoutMatchingSites() {
  var s=new GoogleSearchScraperService(20,1,1){@Override protected BravePage bravePage(String q,int page,String key){return new BravePage(List.of(),true);}};
  try{var result=s.search("q",2,"key",50,2023,2024,p->{});assertTrue(result.leads().isEmpty());assertEquals(2,result.progress().requests());assertEquals("Search request limit reached",result.progress().reason());}finally{s.close();}
 }
 @Test void mapsContinuesBeyondOriginalQueryAndDeduplicates()throws Exception {
  var c=mock(HttpClient.class);doReturn(response(places(0,4,"a")),response(places(4,4,"b")),response(places(8,4,"")),response(places(8,20,""))).when(c).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
  var result=new ScraperService(new ObjectMapper(),c).searchUntilTarget("salons in Delhi","key",25,10,()->false,p->{});
  assertEquals(25,result.leads().size());assertEquals(4,result.progress().requests());assertEquals("Target reached",result.progress().reason());
  var capture=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);verify(c,times(4)).send(capture.capture(),any(HttpResponse.BodyHandler.class));
  assertNotEquals(capture.getAllValues().get(0).bodyPublisher().orElseThrow().contentLength(),capture.getAllValues().get(3).bodyPublisher().orElseThrow().contentLength());
 }
 @Test void copyrightContinuesWithAnotherQueryWhenProviderExhaustsFirst() {
  var queries=new ArrayList<String>();var s=new GoogleSearchScraperService(20,1,4){
   @Override protected BravePage bravePage(String q,int page,String key){queries.add(q);assertEquals(0,page);var items=new ArrayList<GoogleSearchResult>();for(int i=0;i<(queries.size()>1?10:2);i++)items.add(new GoogleSearchResult("https://b"+i+".example","Business",""));return new BravePage(items,false);}
   @Override GoogleSearchLead processWebsite(GoogleSearchResult result){return new GoogleSearchLead(result.url(),"Business",List.of("1234567890"),"2024","");}
  };
  try{var result=s.searchUntilTarget("plumbers in London",10,"key",10,2023,2024,()->false,p->{});assertEquals(10,result.leads().size());assertEquals(List.of("plumbers in London \"copyright 2024\"","plumbers in London \"copyright 2023\""),queries);assertEquals("Target reached",result.progress().reason());}finally{s.close();}
 }
 @Test void cancelAndBudgetPreventFurtherProviderRequests()throws Exception {
  var c=mock(HttpClient.class);var service=new ScraperService(new ObjectMapper(),c);var cancelled=service.searchUntilTarget("q","key",50,10,()->true,p->{});assertTrue(cancelled.progress().reason().contains("Cancelled"));verifyNoInteractions(c);
  doReturn(response(places(0,1,""))).when(c).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));var limited=service.searchUntilTarget("q","key",50,1,()->false,p->{});assertEquals(1,limited.progress().requests());assertTrue(limited.progress().reason().contains("budget"));
 }
 @Test void copyrightQueriesPrioritizeSelectedYearNotBroadDiscovery(){
  var queries=SearchQueries.copyrightVariants("restaurants in Chicago",2023,2024);
  assertEquals("restaurants in Chicago \"copyright 2024\"",queries.getFirst());
  assertEquals("restaurants in Chicago \"copyright 2023\"",queries.get(1));
  assertTrue(queries.indexOf("restaurants in Chicago")>queries.indexOf("restaurants in Chicago \"© 2024\""));
  assertEquals(queries.size(),new HashSet<>(queries).size());assertTrue(queries.stream().allMatch(q->q.contains("restaurants in Chicago")));assertTrue(queries.indexOf("restaurants in Chicago")<=6);
 }
 @Test void slowFirstWebsiteDoesNotDelayAnAlreadyReachedTarget(){
  var release=new java.util.concurrent.CountDownLatch(1);var slowStarted=new java.util.concurrent.CountDownLatch(1);
  var s=new GoogleSearchScraperService(20,1,4){
   @Override protected BravePage bravePage(String q,int page,String key){var items=new ArrayList<GoogleSearchResult>();for(int i=0;i<12;i++)items.add(new GoogleSearchResult("https://b"+i+".example","Business",""));return new BravePage(items,true);}
   @Override GoogleSearchLead processWebsite(GoogleSearchResult r){if(r.url().equals("https://b0.example")){slowStarted.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();return null;}}return new GoogleSearchLead(r.url(),r.title(),List.of("1234567890"),"2024","");}
  };
  try{var result=org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(3),()->s.search("q",1,"key",10,2023,2024,p->{}));assertEquals(10,result.leads().size());assertEquals(0,slowStarted.getCount());assertEquals("Target reached",result.progress().reason());}finally{release.countDown();s.close();}
 }

 @Test void explicitUserCopyrightSearchIsNotRewrittenToContradictAnotherYear(){
  String query="restaurants \"chicago\" \"copyright 2024\"";var queries=SearchQueries.copyrightVariants(query,2023,2024);assertEquals(query,queries.getFirst());assertTrue(queries.stream().noneMatch(q->q.contains("2023")));
 }

}
