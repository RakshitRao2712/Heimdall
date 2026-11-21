package com.scraper.leadscraper.history;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

class LeadExclusionsTest {
    @TempDir Path directory;
    SearchHistory.Lead lead(String name,String phone,String address) {
        return new SearchHistory.Lead(name,"",List.of(phone),address,"",null);
    }
    @Test void selectedFilesAreAccountScopedAndMatchNormalizedBusinessDetails() {
        var history = new SearchHistory(directory.resolve("history").toString(),new ObjectMapper());
        var files = new LeadFiles(directory.resolve("files").toString(),new ObjectMapper(),history);
        var run = history.save("alice","maps","cafes",1,List.of(lead("Joe’s Café","(212) 555-1234","12 Main St.")));
        var file = files.create("alice","Already contacted",run.id());
        var exclusions = new LeadExclusions(files);
        var match = exclusions.fromFiles("alice",List.of(file.id(),file.id()));
        assertTrue(match.test(lead("JOE’S CAFÉ","2125551234","Changed address")));
        assertTrue(match.test(lead("Joe’s Café","","12 main st")));
        assertFalse(match.test(lead("Joe’s Café","2125559876","55 Other St")));
        assertFalse(match.test(lead("Other Cafe","2125551234","55 Other St")));
        assertFalse(exclusions.fromFiles("alice",List.of()).test(run.leads().getFirst()));
        assertThrows(ResponseStatusException.class,()->exclusions.fromFiles("bob",List.of(file.id())));
        files.delete("alice",file.id());
        assertThrows(ResponseStatusException.class,()->exclusions.fromFiles("alice",List.of(file.id())));
    }
    @Test void providerIdMatchesEvenWhenBusinessDetailsChangeAndNameAloneDoesNot() {
        var original=new SearchHistory.Lead("Old name","",List.of(),"","",null,List.of(),"google:123");
        var renamed=new SearchHistory.Lead("New name","",List.of(),"","",null,List.of(),"google:123");
        assertFalse(java.util.Collections.disjoint(LeadExclusions.keys(original),LeadExclusions.keys(renamed)));
        assertTrue(LeadExclusions.keys(lead("A chain","","")).isEmpty());
    }
}
