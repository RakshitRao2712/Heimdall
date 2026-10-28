package com.scraper.leadscraper.history;
import com.scraper.leadscraper.dto.GoogleSearchLead;
import com.scraper.leadscraper.auth.PlacesKeyController;
import org.springframework.mock.web.MockHttpSession;
import com.scraper.leadscraper.dto.PlaceResponse;
import com.scraper.leadscraper.service.GoogleSearchScraperService;
import com.scraper.leadscraper.service.ScraperService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class WorkspaceControllerTest {
    @TempDir Path directory;
    @Test void bothSearchMethodsSaveNormalizedResultsUnderAuthenticatedAccount() {
        var maps = mock(ScraperService.class); var copyright = mock(GoogleSearchScraperService.class);
        var history = new SearchHistory(directory.toString(), new ObjectMapper());
        var controller = new WorkspaceController(history, maps, copyright);
        var session = new MockHttpSession();
        new PlacesKeyController().save(new PlacesKeyController.KeyInput("test-api-key-1234567890123456"), session);
        when(maps.getLeadsWithoutWebsite("cafes in Seattle", "test-api-key-1234567890123456")).thenReturn(List.of(new PlaceResponse.Place(new PlaceResponse.Place.DisplayName("Cafe"), null, "Seattle", "1234567890", "OPERATIONAL", 2)));
        when(copyright.searchAndExtractLeads("plumbers in Chicago", 2)).thenReturn(List.of(new GoogleSearchLead("https://example.com", "Plumber", List.of("2345678901"), "2024", "Public business")));
        var first = controller.maps("cafes in Seattle", () -> "alice@example.com", session);
        var second = controller.copyright("plumbers in Chicago", 2, () -> "alice@example.com");
        assertEquals("maps", first.method()); assertEquals("Seattle", first.leads().getFirst().address());
        assertEquals("copyright", second.method()); assertEquals("2024", second.leads().getFirst().year());
        assertEquals(2, controller.history(() -> "alice@example.com").size());
        assertTrue(controller.history(() -> "bob@example.com").isEmpty());
        assertEquals(second.id(), controller.history(() -> "alice@example.com").getFirst().id());
    }
}
