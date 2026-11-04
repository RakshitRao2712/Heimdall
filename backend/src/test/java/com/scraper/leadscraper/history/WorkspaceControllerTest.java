package com.scraper.leadscraper.history;
import com.scraper.leadscraper.dto.GoogleSearchLead;
import com.scraper.leadscraper.auth.PlacesKeyController;
import com.scraper.leadscraper.auth.ProviderKeyInput;
import com.scraper.leadscraper.auth.BraveKeyController;
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
    private MockHttpSession braveSession() { var session = new MockHttpSession(); new BraveKeyController().save(new BraveKeyController.KeyInput("test-brave-key-1234567890123456"), session); return session; }
    @TempDir Path directory;
    @Test void bothSearchMethodsSaveNormalizedResultsUnderAuthenticatedAccount() {
        var maps = mock(ScraperService.class); var copyright = mock(GoogleSearchScraperService.class);
        var history = new SearchHistory(directory.toString(), new ObjectMapper());
        var controller = new WorkspaceController(history, maps, copyright);
        var session = new MockHttpSession();
        new PlacesKeyController().save(new ProviderKeyInput("test-api-key-1234567890123456"), session);
        when(maps.getLeadsWithoutWebsite("cafes in Seattle", "test-api-key-1234567890123456")).thenReturn(List.of(new PlaceResponse.Place(new PlaceResponse.Place.DisplayName("Cafe"), null, "Seattle", "1234567890", "OPERATIONAL", 2)));
        when(copyright.searchAndExtractLeads("plumbers in Chicago", 2, "test-brave-key-1234567890123456")).thenReturn(List.of(new GoogleSearchLead("https://example.com", "Plumber", List.of("2345678901"), "2024", "Public business")));
        var first = controller.maps("cafes in Seattle", () -> "alice@example.com", session);
        var second = controller.copyright("plumbers in Chicago", 2, () -> "alice@example.com", braveSession());
        assertEquals("maps", first.method()); assertEquals("Seattle", first.leads().getFirst().address()); assertEquals(List.of("1234567890"), first.leads().getFirst().phones());
        assertEquals("copyright", second.method()); assertEquals("2024", second.leads().getFirst().year());
        assertEquals(2, controller.history(() -> "alice@example.com").size());
        assertTrue(controller.history(() -> "bob@example.com").isEmpty());
        assertEquals(second.id(), controller.history(() -> "alice@example.com").getFirst().id());
    }

    @Test void providerFailureIsNotStoredAsAnEmptySuccessfulSearch() {
        var maps = mock(ScraperService.class); var copyright = mock(GoogleSearchScraperService.class);
        var controller = new WorkspaceController(new SearchHistory(directory.toString(), new ObjectMapper()), maps, copyright);
        when(copyright.searchAndExtractLeads("test", 2, "test-brave-key-1234567890123456")).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, "Google challenge"));
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> controller.copyright("test", 2, () -> "a@example.com", braveSession()));
        assertTrue(controller.history(() -> "a@example.com").isEmpty());
        assertEquals(502, controller.error(error).getStatusCode().value());
        assertEquals(java.util.Map.of("detail", "Google challenge"), controller.error(error).getBody());
    }
    @Test void genuineZeroResultsAreSavedAndAccountScoped() {
        var copyright = mock(GoogleSearchScraperService.class);
        when(copyright.searchAndExtractLeads("rare business", 1, "test-brave-key-1234567890123456")).thenReturn(List.of());
        var controller = new WorkspaceController(new SearchHistory(directory.toString(), new ObjectMapper()), mock(ScraperService.class), copyright);
        var run = controller.copyright("rare business", 1, () -> "a@example.com", braveSession());
        assertTrue(run.leads().isEmpty());assertEquals(1, controller.history(() -> "a@example.com").size());assertTrue(controller.history(() -> "other@example.com").isEmpty());
    }
    @Test void targetEndpointSavesProgressAndUsesSessionKey() {
        var maps=mock(ScraperService.class);var controller=new WorkspaceController(new SearchHistory(directory.toString(),new ObjectMapper()),maps,mock(GoogleSearchScraperService.class));
        var session=new MockHttpSession();new PlacesKeyController().save(new ProviderKeyInput("test-api-key-1234567890123456"),session);
        var progress=new com.scraper.leadscraper.service.SearchProgress(50,60,3,0,"complete","Google Places limit reached");
        when(maps.searchUntilTarget(eq("barbers"),eq("test-api-key-1234567890123456"),eq(50),eq(10),any(),any())).thenReturn(new com.scraper.leadscraper.service.SearchProgress.Outcome<>(List.of(),progress));
        var run=controller.mapsUntilTarget("barbers",50,10,()->"alice@example.com",session);assertEquals(progress,run.progress());assertEquals(progress,controller.history(()->"alice@example.com").getFirst().progress());
    }
    @Test void serpSearchUsesSeparateSessionKeyAndPersistsProviderForFiles() {
        var service=mock(GoogleSearchScraperService.class);var history=new SearchHistory(directory.toString(),new ObjectMapper());
        var controller=new WorkspaceController(history,mock(ScraperService.class),service);var session=new MockHttpSession();
        var keys=new com.scraper.leadscraper.auth.SerpKeyController();keys.save(new com.scraper.leadscraper.auth.SerpKeyController.KeyInput("serp-key-12345678901234567890"),session);
        var state=new com.scraper.leadscraper.service.SearchProgress(10,1,1,1,"complete","Provider results exhausted");
        var lead=new GoogleSearchLead("https://business.example","Business",List.of("1234567890"),"2024","");
        when(service.searchSerpUntilTarget(eq("cafes"),eq(50),eq("serp-key-12345678901234567890"),eq(10),eq(2023),eq(2024),any(),any())).thenReturn(new com.scraper.leadscraper.service.SearchProgress.Outcome<>(List.of(lead),state));
        var run=controller.serpTarget("cafes",50,10,2023,2024,()->"alice@example.com",session);
        assertEquals("serp",run.method());assertEquals("Business",run.leads().getFirst().name());assertEquals(state,run.progress());assertTrue(history.list("bob@example.com").isEmpty());
        var files=new LeadFiles(directory.resolve("files").toString(),new ObjectMapper(),history);assertEquals("serp",files.create("alice@example.com","Serp comparison",run.id()).method());
    }
}
