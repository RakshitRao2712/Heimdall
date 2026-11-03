package com.scraper.leadscraper.history;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class LeadFilesTest {
 @TempDir Path directory;
 @Test void snapshotsPersistAndAreAccountScopedBeyondSearchRetention(){var mapper=new ObjectMapper();var history=new SearchHistory(directory.resolve("history").toString(),mapper);var store=new LeadFiles(directory.resolve("files").toString(),mapper,history);var lead=new SearchHistory.Lead("Business","https://example.com",List.of("1234567890"),"City","2024",null);var run=history.save("a@example.com","copyright","plumbers",25,List.of(lead));assertThrows(ResponseStatusException.class,()->store.create("b@example.com","Not yours",run.id()));var saved=store.create("a@example.com","  Chicago plumbers  ",run.id());assertEquals("Chicago plumbers",saved.name());for(int i=0;i<51;i++)history.save("a@example.com","maps","new "+i,10,List.of());var reopened=new LeadFiles(directory.resolve("files").toString(),mapper,history);assertEquals(lead,reopened.list("a@example.com").getFirst().leads().getFirst());assertTrue(reopened.list("b@example.com").isEmpty());assertThrows(ResponseStatusException.class,()->reopened.rename("b@example.com",saved.id(),"No"));assertEquals("Renamed",reopened.rename("a@example.com",saved.id(),"Renamed").name());assertThrows(ResponseStatusException.class,()->reopened.rename("a@example.com",saved.id()," "));}
 @Test void deleteIsAccountScopedPersistsAndPreservesSearchHistory(){
  var mapper=new ObjectMapper();var history=new SearchHistory(directory.resolve("history").toString(),mapper);var store=new LeadFiles(directory.resolve("files").toString(),mapper,history);
  var run=history.save("alice","maps","salons",1,List.of(new SearchHistory.Lead("Salon",null,List.of("1234567890"),"Delhi","",null)));
  assertEquals("Salon",history.list("alice").getFirst().leads().getFirst().name());
  assertEquals(List.of("1234567890"),history.list("alice").getFirst().leads().getFirst().phones());
  var file=store.create("alice","Salons",run.id());var other=store.create("alice","Keep",run.id());
  assertEquals(404,assertThrows(ResponseStatusException.class,()->store.delete("bob",file.id())).getStatusCode().value());assertEquals(2,store.list("alice").size());
  store.delete("alice",file.id());var reopened=new LeadFiles(directory.resolve("files").toString(),mapper,history);assertEquals(List.of(other),reopened.list("alice"));assertEquals("Salon",reopened.list("alice").getFirst().leads().getFirst().name());assertEquals(List.of("1234567890"),reopened.list("alice").getFirst().leads().getFirst().phones());assertEquals(1,history.list("alice").size());assertThrows(ResponseStatusException.class,()->store.delete("alice",file.id()));
 }
}
