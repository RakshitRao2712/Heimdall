package com.scraper.leadscraper.history;

import com.scraper.leadscraper.service.GoogleSearchScraperService;
import com.scraper.leadscraper.service.ScraperService;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import jakarta.servlet.http.HttpSession;
import com.scraper.leadscraper.auth.PlacesKeyController;
import java.util.Map;

@RestController
@RequestMapping("/api/workspace")
public class WorkspaceController {
    private final SearchHistory history;
    private final ScraperService maps;
    private final GoogleSearchScraperService copyright;
    public WorkspaceController(SearchHistory history, ScraperService maps, GoogleSearchScraperService copyright) { this.history = history; this.maps = maps; this.copyright = copyright; }
    @GetMapping("/history") public List<SearchHistory.Run> history(Principal user) { return history.list(user.getName()); }
    @GetMapping("/search/maps") public SearchHistory.Run maps(@RequestParam String query, Principal user, HttpSession session) {
        long start = System.nanoTime();
        var leads = maps.getLeadsWithoutWebsite(query, PlacesKeyController.requireKey(session)).stream().map(p -> new SearchHistory.Lead(p.displayName() == null ? "Unnamed business" : p.displayName().text(), p.websiteUri(), p.nationalPhoneNumber() == null ? List.of() : List.of(p.nationalPhoneNumber()), p.formattedAddress(), "", p.userRatingCount())).toList();
        return history.save(user.getName(), "maps", query, (System.nanoTime() - start) / 1_000_000, leads);
    }
    @GetMapping("/search/copyright") public SearchHistory.Run copyright(@RequestParam String query, @RequestParam(defaultValue="2") int pages, Principal user) {
        long start = System.nanoTime();
        var leads = copyright.searchAndExtractLeads(query, pages).stream().map(p -> new SearchHistory.Lead(p.businessName(), p.websiteUrl(), p.phoneNumbers(), "", p.copyrightYear(), null)).toList();
        return history.save(user.getName(), "copyright", query, (System.nanoTime() - start) / 1_000_000, leads);
    }
}
