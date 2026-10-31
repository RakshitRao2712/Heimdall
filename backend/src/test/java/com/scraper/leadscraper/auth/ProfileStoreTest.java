package com.scraper.leadscraper.auth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ProfileStoreTest {
    @TempDir Path directory;
    @Test void completionPersistsAndIsAccountScoped() {
        var store = new ProfileStore(directory.toString(), new ObjectMapper());
        assertFalse(store.get("a@example.com").completed());
        var saved = store.save("a@example.com", new ProfileStore.Profile(" Alex ", "", "alex", "https://example.com", false));
        assertEquals("Alex", saved.firstName()); assertEquals("example.com", saved.website()); assertTrue(saved.completed());
        assertEquals(saved, new ProfileStore(directory.toString(), new ObjectMapper()).get("a@example.com"));
        assertFalse(store.get("b@example.com").completed());
        assertThrows(ResponseStatusException.class, () -> store.save("a@example.com", new ProfileStore.Profile("", "", "name", "", true)));
        assertThrows(ResponseStatusException.class, () -> store.save("a@example.com", new ProfileStore.Profile("Alex", "", "", "", true)));
        assertThrows(ResponseStatusException.class, () -> store.save("a@example.com", new ProfileStore.Profile("Alex", "", "alex", "javascript:bad", true)));
        assertEquals(saved, store.get("a@example.com"));
    }
    @Test void displayNamesAreUniqueIgnoringCaseSpacingAndUnicodeWidth() {
        var store = new ProfileStore(directory.toString(), new ObjectMapper());
        store.save("a@example.com", new ProfileStore.Profile("Alex", "", "Alex  Smith", "", true));
        assertTrue(store.available("a@example.com", "alex smith"));
        assertFalse(store.available("b@example.com", " ALEX   SMITH "));
        assertFalse(store.available("b@example.com", "Ａｌｅｘ Ｓｍｉｔｈ"));
        var conflict = assertThrows(ResponseStatusException.class, () -> store.save("b@example.com", new ProfileStore.Profile("Other", "", "alex smith", "", true)));
        assertEquals(409, conflict.getStatusCode().value());
        store.save("a@example.com", new ProfileStore.Profile("Alex", "", "Alex Smith", "", true));
        store.save("a@example.com", new ProfileStore.Profile("Alex", "", "Alex Jones", "", true));
        assertTrue(store.available("b@example.com", "Alex Smith"));
    }
    @Test void concurrentClaimsHaveExactlyOneWinner() throws Exception {
        var store = new ProfileStore(directory.toString(), new ObjectMapper());
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> first = () -> { try { store.save("a@example.com", new ProfileStore.Profile("A", "", "Unique Name", "", true)); return true; } catch (ResponseStatusException e) { return false; } };
            java.util.concurrent.Callable<Boolean> second = () -> { try { store.save("b@example.com", new ProfileStore.Profile("B", "", "unique name", "", true)); return true; } catch (ResponseStatusException e) { return false; } };
            var results = executor.invokeAll(java.util.List.of(first, second));
            assertEquals(1, (results.get(0).get() ? 1 : 0) + (results.get(1).get() ? 1 : 0));
        }
    }
}
