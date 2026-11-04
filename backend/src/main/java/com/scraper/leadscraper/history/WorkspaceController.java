package com.scraper.leadscraper.history;

import com.scraper.leadscraper.service.GoogleSearchScraperService;
import com.scraper.leadscraper.service.ScraperService;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import jakarta.servlet.http.HttpSession;
import com.scraper.leadscraper.auth.PlacesKeyController;
import com.scraper.leadscraper.auth.BraveKeyController;
import java.util.Map;

@RestController
@RequestMapping("/api/workspace")
public class WorkspaceController {
    private final SearchHistory history;
    private final ScraperService maps;
    private final GoogleSearchScraperService copyright;
    public WorkspaceController(SearchHistory history, ScraperService maps, GoogleSearchScraperService copyright) { this.history = history; this.maps = maps; this.copyright = copyright; }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<?> error(org.springframework.web.server.ResponseStatusException e) { return org.springframework.http.ResponseEntity.status(e.getStatusCode()).body(Map.of("detail", e.getReason() == null ? "Search could not complete" : e.getReason())); }
    @GetMapping("/history") public List<SearchHistory.Run> history(Principal user) { return history.list(user.getName()); }
    public SearchHistory.Run maps(@RequestParam String query, Principal user, HttpSession session) {
        long start = System.nanoTime();
        var leads = maps.getLeadsWithoutWebsite(query, PlacesKeyController.requireKey(session)).stream().map(p -> new SearchHistory.Lead(p.displayName() == null ? "Unnamed business" : p.displayName().text(), p.websiteUri(), p.nationalPhoneNumber() == null ? List.of() : List.of(p.nationalPhoneNumber()), p.formattedAddress(), "", p.userRatingCount())).toList();
        return history.save(user.getName(), "maps", query, (System.nanoTime() - start) / 1_000_000, leads);
    }
    public SearchHistory.Run copyright(@RequestParam String query, @RequestParam(defaultValue="2") int pages, Principal user, HttpSession session) {
        long start = System.nanoTime();
        var leads = copyright.searchAndExtractLeads(query, pages, BraveKeyController.requireKey(session)).stream().map(p -> new SearchHistory.Lead(p.businessName(), p.websiteUrl(), p.phoneNumbers(), "", p.copyrightYear(), null)).toList();
        return history.save(user.getName(), "copyright", query, (System.nanoTime() - start) / 1_000_000, leads);
    }
    private final java.util.Set<String> activeAccounts=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final String PROGRESS=WorkspaceController.class.getName()+".progress";
    @GetMapping("/search/progress") public Object progress(HttpSession session) {
        Object progress=session.getAttribute(PROGRESS);return progress==null?Map.of("status","idle"):progress;
    }
    public SearchHistory.Run mapsTarget(@RequestParam String query,@RequestParam(defaultValue="10") int target,Principal user,HttpSession session) {
        return execute("maps",query,user,session,()-> {
            var result=maps.search(query,PlacesKeyController.requireKey(session),target,p->session.setAttribute(PROGRESS,p));
            var leads=result.leads().stream().map(p->new SearchHistory.Lead(p.displayName()==null?"Unnamed business":p.displayName().text(),p.websiteUri(),p.nationalPhoneNumber()==null?List.<String>of():List.of(p.nationalPhoneNumber()),p.formattedAddress(),"",p.userRatingCount())).toList();
            return new com.scraper.leadscraper.service.SearchProgress.Outcome<>(leads,result.progress());
        });
    }
    @PostMapping("/search/maps") public SearchHistory.Run mapsUntilTarget(@RequestParam String query,@RequestParam(defaultValue="10") int target,@RequestParam(defaultValue="50") int maxRequests,Principal user,HttpSession session) {
        return execute("maps",query,user,session,()-> {
            var result=maps.searchUntilTarget(query,PlacesKeyController.requireKey(session),target,maxRequests,()->cancelled(session),p->session.setAttribute(PROGRESS,p));
            var leads=result.leads().stream().map(p->new SearchHistory.Lead(p.displayName()==null?"Unnamed business":p.displayName().text(),p.websiteUri(),p.nationalPhoneNumber()==null?List.<String>of():List.of(p.nationalPhoneNumber()),p.formattedAddress(),"",p.userRatingCount())).toList();
            return new com.scraper.leadscraper.service.SearchProgress.Outcome<>(leads,result.progress());
        });
    }
    private boolean cancelled(HttpSession session) { Object value=session.getAttribute(PROGRESS+".cancel");return value instanceof java.util.concurrent.atomic.AtomicBoolean flag&&flag.get(); }
    @PostMapping("/search/cancel") public Map<String,Boolean> cancel(HttpSession session) {Object value=session.getAttribute(PROGRESS+".cancel");if(value instanceof java.util.concurrent.atomic.AtomicBoolean flag)flag.set(true);return Map.of("cancelled",true);}
    @PostMapping("/search/copyright") public SearchHistory.Run copyrightTarget(@RequestParam String query,@RequestParam(defaultValue="50") int maxRequests,@RequestParam(defaultValue="10") int target,@RequestParam(defaultValue="2023") int minYear,@RequestParam(defaultValue="2024") int maxYear,Principal user,HttpSession session) {
        return execute("copyright",query,user,session,()-> {
            var result=copyright.searchUntilTarget(query,maxRequests,BraveKeyController.requireKey(session),target,minYear,maxYear,()->cancelled(session),p->session.setAttribute(PROGRESS,p));
            var leads=result.leads().stream().map(p->new SearchHistory.Lead(p.businessName(),p.websiteUrl(),p.phoneNumbers(),"",p.copyrightYear(),null)).toList();
            return new com.scraper.leadscraper.service.SearchProgress.Outcome<>(leads,result.progress());
        });
    }
    @PostMapping("/search/serp") public SearchHistory.Run serpTarget(@RequestParam String query,@RequestParam(defaultValue="50") int maxRequests,@RequestParam(defaultValue="10") int target,@RequestParam(defaultValue="2023") int minYear,@RequestParam(defaultValue="2024") int maxYear,Principal user,HttpSession session) {
        return execute("serp",query,user,session,()-> {
            var result=copyright.searchSerpUntilTarget(query,maxRequests,com.scraper.leadscraper.auth.SerpKeyController.requireKey(session),target,minYear,maxYear,()->cancelled(session),p->session.setAttribute(PROGRESS,p));
            var leads=result.leads().stream().map(p->new SearchHistory.Lead(p.businessName(),p.websiteUrl(),p.phoneNumbers(),"",p.copyrightYear(),null)).toList();
            return new com.scraper.leadscraper.service.SearchProgress.Outcome<>(leads,result.progress());
        });
    }
    @PostMapping("/search/browser") public SearchHistory.Run browserTarget(@RequestParam String query,@RequestParam(defaultValue="10") int maxRequests,@RequestParam(defaultValue="10") int target,@RequestParam(defaultValue="2023") int minYear,@RequestParam(defaultValue="2024") int maxYear,Principal user,HttpSession session) {
        return execute("browser",query,user,session,()-> {
            var result=copyright.searchBrowserUntilTarget(query,maxRequests,target,minYear,maxYear,()->cancelled(session),p->session.setAttribute(PROGRESS,p),browser->com.scraper.leadscraper.service.BrowserVerificationController.await(session,browser,()->cancelled(session)));
            var leads=result.leads().stream().map(p->new SearchHistory.Lead(p.businessName(),p.websiteUrl(),p.phoneNumbers(),"",p.snippet().startsWith("Google snippet only (unverified): ") ? p.copyrightYear() + " (Google snippet, unverified)" : p.copyrightYear(),null)).toList();
            return new com.scraper.leadscraper.service.SearchProgress.Outcome<>(leads,result.progress());
        });
    }
    @PostMapping("/search/yelp")
    public SearchHistory.Run yelp(@RequestParam String query, Principal user, HttpSession session) {
        String key = com.scraper.leadscraper.auth.YelpKeyController.requireKey(session);
        return execute("yelp", query, user, session, () ->
                com.scraper.leadscraper.service.YelpDiscovery.search(query, key, () -> cancelled(session),
                        progress -> session.setAttribute(PROGRESS, progress)));
    }

    @PostMapping("/search/foursquare")
    public SearchHistory.Run foursquare(@RequestParam String query, @RequestParam(defaultValue = "50") int target, Principal user, HttpSession session) {
        String key = com.scraper.leadscraper.auth.FoursquareKeyController.requireKey(session);
        return execute("foursquare", query, user, session, () ->
                com.scraper.leadscraper.service.FoursquareDiscovery.search(query, key, target, () -> cancelled(session),
                        progress -> session.setAttribute(PROGRESS, progress)));
    }

    private SearchHistory.Run execute(String method,String query,Principal user,HttpSession session,java.util.function.Supplier<com.scraper.leadscraper.service.SearchProgress.Outcome<SearchHistory.Lead>> operation) {
        if(!activeAccounts.add(user.getName()))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"A search is already running for this account.");
        synchronized(session) {session.setAttribute(PROGRESS+".busy",true);session.removeAttribute(PROGRESS);session.setAttribute(PROGRESS+".cancel",new java.util.concurrent.atomic.AtomicBoolean());}
        long start=System.nanoTime();
        try {var result=operation.get();long elapsedMs = (System.nanoTime()-start)/1_000_000;
            if (method.equals("yelp") || method.equals("foursquare")) {
                return new SearchHistory.Run(java.util.UUID.randomUUID().toString(), method, query,
                        java.time.Instant.now().toString(), elapsedMs, result.leads(), result.progress());
            }
            return history.save(user.getName(), method, query, elapsedMs, result.leads(), result.progress());}
        catch(RuntimeException e){session.setAttribute(PROGRESS,Map.of("status","error","reason","Search failed. See the search error for details."));throw e;}
        finally {activeAccounts.remove(user.getName());session.removeAttribute(PROGRESS+".busy");}
    }
}
