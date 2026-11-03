package com.scraper.leadscraper.service;
import com.scraper.leadscraper.dto.GoogleSearchLead;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PlaywrightGoogleDiscoveryTest {
 @Test void exactQueryPagingAndTargetWorkWithoutAnApiKey(){
  var browser=mock(PlaywrightGoogleDiscovery.class);String query="restaurants \"chicago\" \"copyright 2024\"";
  var service=new GoogleSearchScraperService(20,1,4){
   @Override protected PlaywrightGoogleDiscovery openBrowser(){return browser;}
   @Override GoogleSearchLead processWebsite(GoogleSearchResult r){return new GoogleSearchLead(r.url(),r.title(),List.of("1234567890"),"2024","");}
  };
  var html=new StringBuilder();for(int i=0;i<12;i++)html.append("<a href='https://b"+i+".example'><h3>Restaurant "+i+"</h3></a>");
  when(browser.page(query,0)).thenReturn(html.toString());
  try {var result=service.searchBrowserUntilTarget(query,10,10,2023,2024,()->false,p->{});assertEquals(10,result.leads().size());assertEquals(1,result.progress().requests());assertEquals("Target reached",result.progress().reason());verify(browser).page(query,0);verify(browser).close();}finally{service.close();}
  var uri=PlaywrightGoogleDiscovery.searchUri(query,2);assertEquals("www.google.com",uri.getHost());assertTrue(uri.getRawQuery().contains("start=20"));assertTrue(java.net.URLDecoder.decode(uri.getRawQuery(),java.nio.charset.StandardCharsets.UTF_8).contains(query));
 }
 @Test void cancellationDoesNotLaunchBrowserAndChallengeIsNotBypassed(){
  var service=new GoogleSearchScraperService(20,1,1){@Override protected PlaywrightGoogleDiscovery openBrowser(){throw new AssertionError("Do not launch");}};
  try{assertTrue(service.searchBrowserUntilTarget("q",10,50,2023,2024,()->true,p->{}).progress().reason().contains("Cancelled"));}finally{service.close();}
  assertTrue(GoogleSearchScraperService.isBlocked("https://www.google.com/sorry/",""));
  assertTrue(GoogleSearchScraperService.isBlocked("https://www.google.com/search","Our systems have detected unusual traffic"));
 }
 @Test void plainBusinessQueryUsesSelectedYearsAndMovesPastEmptyResults(){
  var browser=mock(PlaywrightGoogleDiscovery.class);
  var service=new GoogleSearchScraperService(20,1,1){@Override protected PlaywrightGoogleDiscovery openBrowser(){return browser;}};
  when(browser.page(anyString(),anyInt())).thenReturn("<p>did not match any documents</p>");
  try{
   var result=service.searchBrowserUntilTarget("plumbers in Chicago",2,10,2023,2024,()->false,p->{});
   var order=inOrder(browser);
   order.verify(browser).page("plumbers \"Chicago\" \"copyright 2024\"",0);
   order.verify(browser).page("plumbers \"Chicago\" \"copyright 2023\"",0);
   assertEquals(2,result.progress().requests());
   assertTrue(result.progress().reason().contains("No eligible website links"));
   verify(browser,never()).page("plumbers in Chicago",0);
  }finally{service.close();}
 }
 @Test void browserQueriesRemainYearQualifiedAndRespectExplicitQuery(){
  var queries=SearchQueries.browserCopyrightVariants("restaurants in Chicago",2023,2024);
  assertEquals(List.of("restaurants \"Chicago\" \"copyright 2024\"",
      "restaurants \"Chicago\" \"copyright 2023\""), queries);
  String explicit="restaurants \"chicago\" \"copyright 2024\"";
  assertEquals(List.of(explicit),SearchQueries.browserCopyrightVariants(explicit,2023,2024));
 }
}
