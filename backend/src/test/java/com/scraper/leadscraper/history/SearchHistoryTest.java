package com.scraper.leadscraper.history;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class SearchHistoryTest {
    @TempDir Path directory;
    @Test void historyPersistsPerAccountAndRetainsLatestFifty() {
        var store = new SearchHistory(directory.toString(), new ObjectMapper());
        var lead = new SearchHistory.Lead("A business", "https://example.com", List.of("1234567890"), "A city", "2024", null);
        for (int i = 0; i < 52; i++) store.save("a@example.com", "copyright", "query " + i, 100, List.of(lead));
        var reopened = new SearchHistory(directory.toString(), new ObjectMapper());
        assertEquals(50, reopened.list("a@example.com").size());
        assertEquals("query 51", reopened.list("a@example.com").getFirst().query());
        assertEquals(lead, reopened.list("a@example.com").getFirst().leads().getFirst());
        assertTrue(reopened.list("b@example.com").isEmpty());
    }
}
