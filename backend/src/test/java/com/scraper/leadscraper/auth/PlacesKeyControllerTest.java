package com.scraper.leadscraper.auth;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
class PlacesKeyControllerTest {
    @Test void keysAreSessionScopedRemovableAndNeverReturned() {
        var controller = new PlacesKeyController(); var alice = new MockHttpSession(); var bob = new MockHttpSession();
        assertFalse(controller.status(alice).get("configured"));
        assertThrows(ResponseStatusException.class, () -> PlacesKeyController.requireKey(alice));
        var status = controller.save(new PlacesKeyController.KeyInput("test-api-key-1234567890123456"), alice);
        assertEquals(1, status.size()); assertTrue(status.get("configured"));
        assertEquals("test-api-key-1234567890123456", PlacesKeyController.requireKey(alice));
        assertThrows(ResponseStatusException.class, () -> PlacesKeyController.requireKey(bob));
        controller.remove(alice); assertFalse(controller.status(alice).get("configured"));
        assertThrows(ResponseStatusException.class, () -> controller.save(new PlacesKeyController.KeyInput("invalid"), alice));
    }
}
