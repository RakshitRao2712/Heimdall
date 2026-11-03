package com.scraper.leadscraper.service;

import com.scraper.leadscraper.dto.GoogleSearchLead;
import jakarta.annotation.PreDestroy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GoogleSearchScraperService {
    private static final Logger log = LoggerFactory.getLogger(GoogleSearchScraperService.class);
    // Use the final year of a range, not its start. Require a complete four-digit year.
    private static final Pattern COPYRIGHT = Pattern.compile(
            "(?:©|\\(c\\)|copyright)\\s*(?:©\\s*)?(?:(?:19|20)\\d{2}\\s*[-–—/]\\s*)?((?:19|20)\\d{2})(?:\\s*[-–—/]\\s*(\\d{2}))?(?!\\d)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile(
            "(?<![\\p{L}\\d])\\+?\\d[\\d(). \\u00a0-]{5,25}\\d(?![\\p{L}\\d])");
    private static final Pattern JSON_PHONE = Pattern.compile(
            "\"telephone\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Set<String> EXCLUDED = Set.of(
            "google.com", "googleapis.com", "gstatic.com", "youtube.com", "googleusercontent.com",
            "yelp.com", "tripadvisor.com", "opentable.com", "doordash.com", "grubhub.com",
            "ubereats.com", "facebook.com", "instagram.com", "foursquare.com", "eater.com",
            "theinfatuation.com", "wikipedia.org", "timeout.com", "thrillist.com", "michelin.com",
            "resy.com", "yellowpages.com", "menupix.com", "seamless.com");

    private final int maxResults;
    private final Duration timeout;
    private final HttpClient client;
    private final com.scraper.leadscraper.security.SafeWebsiteFetcher websiteFetcher;
    private final ExecutorService workers;

    public GoogleSearchScraperService(
            @Value("${google.search.max.results:20}") int maxResults,
            @Value("${google.search.page.timeout:10}") int timeoutSeconds,
            @Value("${google.search.concurrency:8}") int concurrency) {
        if (maxResults < 1 || maxResults > 200 || timeoutSeconds < 1 || timeoutSeconds > 60
                || concurrency < 1 || concurrency > 32) {
            throw new IllegalArgumentException("Invalid Google search limits (results 1–200, timeout 1–60, concurrency 1–32)");
        }
        this.maxResults = maxResults;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.client = HttpClient.newBuilder().connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER).build();
        this.websiteFetcher=new com.scraper.leadscraper.security.SafeWebsiteFetcher(timeout);
        this.workers = new ThreadPoolExecutor(concurrency, concurrency, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(200), new ThreadPoolExecutor.AbortPolicy());
    }

    @PreDestroy
    public void close() {
        workers.shutdownNow();
        client.close();
        websiteFetcher.close();
    }

    public List<GoogleSearchLead> searchAndExtractLeads(String query, int pages) {
        if (query == null || query.isBlank() || query.length() > 500 || pages < 1 || pages > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Supply a query of 1–500 characters and pages between 1 and 10");
        }
        return extractResults(scrapeGoogleSearchResults(query.trim(), pages));
    }

    public List<GoogleSearchLead> searchAndExtractLeads(String query, int pages, String apiKey) {
        if (query == null || query.isBlank() || query.length() > 500 || pages < 1 || pages > 10) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supply a query of 1–500 characters and pages between 1 and 10");
        return extractResults(braveResults(query.trim(), pages, apiKey), 2023, 2024, Long.MAX_VALUE, () -> false, Integer.MAX_VALUE, false, true);
    }

    public SearchProgress.Outcome<GoogleSearchLead> search(String query, int pages, String apiKey, int target, int minYear, int maxYear, java.util.function.Consumer<SearchProgress> progress) {
        return searchInternal(query,pages,apiKey,target,minYear,maxYear,false,()->false,progress);
    }
    public SearchProgress.Outcome<GoogleSearchLead> searchUntilTarget(String query,int maxRequests,String apiKey,int target,int minYear,int maxYear,java.util.function.BooleanSupplier cancelled,java.util.function.Consumer<SearchProgress> progress) {
        return searchInternal(query,maxRequests,apiKey,target,minYear,maxYear,true,cancelled,progress);
    }
    public SearchProgress.Outcome<GoogleSearchLead> searchSerpUntilTarget(String query,int maxRequests,String apiKey,int target,int minYear,int maxYear,java.util.function.BooleanSupplier cancelled,java.util.function.Consumer<SearchProgress> progress) {
        return searchInternal(query,maxRequests,apiKey,target,minYear,maxYear,true,cancelled,progress,true);
    }
    private SearchProgress.Outcome<GoogleSearchLead> searchInternal(String query,int pages,String apiKey,int target,int minYear,int maxYear,boolean expand,java.util.function.BooleanSupplier cancelled,java.util.function.Consumer<SearchProgress> progress) {
        return searchInternal(query,pages,apiKey,target,minYear,maxYear,expand,cancelled,progress,false);
    }
    private SearchProgress.Outcome<GoogleSearchLead> searchInternal(String query,int pages,String apiKey,int target,int minYear,int maxYear,boolean expand,java.util.function.BooleanSupplier cancelled,java.util.function.Consumer<SearchProgress> progress,boolean serp) {
        if(query==null||query.isBlank()||query.length()>500||pages<1||pages>(expand?100:10)||!List.of(10,25,50).contains(target)||minYear<1900||maxYear>java.time.Year.now().getValue()||minYear>maxYear) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Check query, target, page limit, and copyright year range.");
        var leads=new LinkedHashMap<String,GoogleSearchLead>();var seen=new java.util.HashSet<String>();int checked=0,requests=0;String reason="Search request limit reached";long deadline=expand?Long.MAX_VALUE:System.nanoTime()+TimeUnit.SECONDS.toNanos(180);
        try {
            var queries=expand?SearchQueries.copyrightVariants(query,minYear,maxYear):List.of(query.trim());
            searchLoop: for(String expandedQuery:queries) {
            for(int page=0;page<(expand?3:pages);page++) {
                if(cancelled.getAsBoolean()){reason="Cancelled; partial results retained";break searchLoop;}
                if(requests>=pages){reason="Search request limit reached; try a more specific query, another location, or a wider year range.";break searchLoop;}
                if(System.nanoTime()>deadline) {reason="Search time limit reached; partial results retained";break;}
                if(requests>0) Thread.sleep(1100); // Avoid bursts against personal API plans.
                if(deadline-System.nanoTime()<TimeUnit.SECONDS.toNanos(20)){reason="Search time limit reached; partial results retained";break;}
                requests++;progress.accept(new SearchProgress(target,checked,requests,leads.size(),"searching","Searching: "+expandedQuery+" · request "+requests));
                var batch=serp?serpPage(expandedQuery,page,apiKey):bravePage(expandedQuery,page,apiKey);
                var fresh=batch.results().stream().filter(item->seen.add(siteKey(URI.create(item.url())))).toList();
                // Bound each processing batch to 20 websites, sharing the existing worker pool.
                var matches=extractResults(fresh,minYear,maxYear,deadline,cancelled,target-leads.size(),false,!serp);checked+=fresh.size();
                for(var lead:matches) {leads.putIfAbsent(siteKey(URI.create(lead.websiteUrl())),lead);if(leads.size()>=target)break;}
                progress.accept(new SearchProgress(target,checked,requests,leads.size(),"searching","Checked websites for copyright and phone numbers"));
                if(leads.size()>=target){reason="Target reached";break searchLoop;}
                if(cancelled.getAsBoolean()){reason="Cancelled; partial results retained";break searchLoop;}
                if(System.nanoTime()>=deadline){reason="Search time limit reached; partial results retained";break;}
                if(!batch.more()){reason="Provider results exhausted for available query variants";break;}
            }
            }
            if(expand && leads.size()<target && requests<pages && !cancelled.getAsBoolean())reason="All available related searches exhausted; not enough qualifying websites were found.";
        } catch(Exception e) {
            if(e instanceof InterruptedException)Thread.currentThread().interrupt();
            if(leads.isEmpty()){if(e instanceof ResponseStatusException status)throw status;throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Search interrupted or unavailable. Retry later.");}
            reason="Stopped early after a provider or network error; partial results retained.";
        }
        var state=new SearchProgress(target,checked,requests,leads.size(),"complete",reason);progress.accept(state);return new SearchProgress.Outcome<>(List.copyOf(leads.values()),state);
    }

    @Value("${heimdall.browser.visible:false}") private boolean visibleBrowser;
    protected PlaywrightGoogleDiscovery openBrowser(){return new PlaywrightGoogleDiscovery(visibleBrowser); }
    public SearchProgress.Outcome<GoogleSearchLead> searchBrowserUntilTarget(String query,int maxRequests,int target,int minYear,int maxYear,java.util.function.BooleanSupplier cancelled,java.util.function.Consumer<SearchProgress> progress) {
        return searchBrowserUntilTarget(query,maxRequests,target,minYear,maxYear,cancelled,progress,null);
    }
    public SearchProgress.Outcome<GoogleSearchLead> searchBrowserUntilTarget(String query,int maxRequests,int target,int minYear,int maxYear,java.util.function.BooleanSupplier cancelled,java.util.function.Consumer<SearchProgress> progress,java.util.function.Consumer<PlaywrightGoogleDiscovery> verification) {
        if(query==null||query.isBlank()||query.length()>500||maxRequests<1||maxRequests>100||!List.of(10,25,50).contains(target)||minYear<1900||maxYear>java.time.Year.now().getValue()||minYear>maxYear)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Check query, lead target and copyright years.");
        var leads=new LinkedHashMap<String,GoogleSearchLead>();var seen=new HashSet<String>();int checked=0,requests=0;
        String reason="Google search pages exhausted; not enough qualifying websites found.";
        if(cancelled.getAsBoolean())return new SearchProgress.Outcome<>(List.of(),new SearchProgress(target,0,0,0,"complete","Cancelled; partial results retained"));
        try(var browser=openBrowser()) {
            searchQueries: for(String browserQuery:SearchQueries.browserCopyrightVariants(query,minYear,maxYear)) {
             for(int page=0;page<10;page++) {
                if(requests>=maxRequests){reason="Browser search request limit reached; partial results retained.";break searchQueries;}
                if(cancelled.getAsBoolean()){reason="Cancelled; partial results retained";break searchQueries;}
                requests++;progress.accept(new SearchProgress(target,checked,requests,leads.size(),"searching","Searching: "+browserQuery+" · page "+(page+1)));
                final int waitingChecked=checked,waitingRequests=requests;
                browser.setVerificationHandler(verification==null?null:()-> {
                    progress.accept(new SearchProgress(target,waitingChecked,waitingRequests,leads.size(),"verification","Google verification required. Open verification to continue."));
                    verification.accept(browser);
                    progress.accept(new SearchProgress(target,waitingChecked,waitingRequests,leads.size(),"searching","Google verification completed; resuming search."));
                });
                String html=browser.page(browserQuery,page);
                var extracted = parseSearchResults(html);
                if (extracted.isEmpty()) {
                    log.info("Google page {} yielded no eligible result links", page + 1);
                    break;
                }
                var candidates = extracted.stream()
                        .filter(item -> seen.add(siteKey(URI.create(item.url())))).toList();
                var matches=extractResults(candidates,minYear,maxYear,Long.MAX_VALUE,cancelled,target-leads.size(),true,false);checked+=candidates.size();
                for(var lead:matches){leads.putIfAbsent(siteKey(URI.create(lead.websiteUrl())),lead);if(leads.size()>=target)break;}
                progress.accept(new SearchProgress(target,checked,requests,leads.size(),"searching","Checked Google websites for copyright and phone numbers"));
                if(leads.size()>=target){reason="Target reached";break searchQueries;}
                if(cancelled.getAsBoolean()){reason="Cancelled; partial results retained";break searchQueries;}
                var doc=Jsoup.parse(html,"https://www.google.com");
                if(doc.select("a#pnnext, a[aria-label=Next], a[aria-label='Next page']").isEmpty())break;
                Thread.sleep(1100);
            }
            }
        }catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();if(cancelled.getAsBoolean())reason="Cancelled; partial results retained";else {if(leads.isEmpty()){if(e instanceof ResponseStatusException status)throw status;throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Browser search interrupted. Retry later.");}reason="Browser stopped after a verification or network error; partial results retained.";}}
        if (checked == 0 && reason.startsWith("Google search pages exhausted")) {
            reason = "No eligible website links could be extracted from Google results.";
        }
        var state=new SearchProgress(target,checked,requests,leads.size(),"complete",reason);progress.accept(state);return new SearchProgress.Outcome<>(List.copyOf(leads.values()),state);
    }

    private List<GoogleSearchLead> extractResults(List<GoogleSearchResult> results) { return extractResults(results,2023,2024); }
    private List<GoogleSearchLead> extractResults(List<GoogleSearchResult> results, int minYear, int maxYear) { return extractResults(results,minYear,maxYear,Long.MAX_VALUE); }
    private List<GoogleSearchLead> extractResults(List<GoogleSearchResult> results,int minYear,int maxYear,long deadline) { return extractResults(results,minYear,maxYear,deadline,()->false); }
    private List<GoogleSearchLead> extractResults(List<GoogleSearchResult> results,int minYear,int maxYear,long deadline,java.util.function.BooleanSupplier cancelled) {return extractResults(results,minYear,maxYear,deadline,cancelled,Integer.MAX_VALUE);}
    private List<GoogleSearchLead> extractResults(List<GoogleSearchResult> results,int minYear,int maxYear,long deadline,java.util.function.BooleanSupplier cancelled,int needed) {
        return extractResults(results,minYear,maxYear,deadline,cancelled,needed,false,false);
    }
    private List<GoogleSearchLead> extractResults(List<GoogleSearchResult> results,int minYear,int maxYear,long deadline,java.util.function.BooleanSupplier cancelled,int needed,boolean allowSnippet,boolean phoneOptional) {
        long started = System.nanoTime();
        List<Future<GoogleSearchLead>> tasks = new ArrayList<>();
        var completions=new ExecutorCompletionService<GoogleSearchLead>(workers);
        List<GoogleSearchLead> leads = new ArrayList<>();
        try {
            for (GoogleSearchResult result : results) {
                tasks.add(completions.submit(() -> {
                    var verified = minYear==2023 && maxYear==2024 ? processWebsite(result) : processWebsite(result,minYear,maxYear);
                    if (verified != null && !phoneOptional && verified.phoneNumbers().isEmpty()) verified = null;
                    if (verified != null || !allowSnippet || cancelled.getAsBoolean()) return verified;
                    return snippetLead(result,minYear,maxYear);
                }));
            }
            for (int completed=0;completed<tasks.size()&&leads.size()<needed;completed++) {
                Future<GoogleSearchLead> task=null;
                while(task==null){
                    if(cancelled.getAsBoolean())return leads;
                    long remaining=deadline-System.nanoTime();if(remaining<=0)return leads;
                    task=completions.poll(Math.min(remaining,TimeUnit.SECONDS.toNanos(1)),TimeUnit.NANOSECONDS);
                }
                var lead=task.get();if(lead!=null)leads.add(lead);
            }
            log.info("Checked {} websites; found {} leads in {} ms", results.size(), leads.size(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
            return leads;
        } catch (RejectedExecutionException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Scraper busy; retry shortly", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Scrape interrupted", e);
        } catch (ExecutionException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Website processing failed", e);
        } finally {
            tasks.forEach(task -> { if (!task.isDone()) task.cancel(true); });
        }
    }

    protected List<GoogleSearchResult> scrapeGoogleSearchResults(String query, int pages) {
        throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,"Connect your Brave Search API key before searching.");
    }

    record BravePage(List<GoogleSearchResult> results,boolean more) { }
    protected BravePage bravePage(String query,int page,String apiKey) {
        if(apiKey==null||apiKey.isBlank())throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,"Connect your Brave Search API key before searching.");
        try {
            var request=HttpRequest.newBuilder(URI.create("https://api.search.brave.com/res/v1/web/search?q="+URLEncoder.encode(query,StandardCharsets.UTF_8)+"&count=20&offset="+page)).timeout(Duration.ofSeconds(20)).header("Accept","application/json").header("X-Subscription-Token",apiKey).GET().build();
            var response=client.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,response.statusCode()==429?"Brave Search quota or rate limit reached. Check your plan and retry later.":"Brave Search rejected the request. Check your key and active Search plan.");
            var json=new tools.jackson.databind.ObjectMapper().readTree(response.body());var results=new ArrayList<GoogleSearchResult>();
            for(var item:json.path("web").path("results")) {try {var uri=URI.create(item.path("url").asText());if(isWebsite(uri)&&!excluded(uri.getHost()))results.add(new GoogleSearchResult(uri.toString(),item.path("title").asText(),item.path("description").asText()));}catch(IllegalArgumentException ignored){}}
            return new BravePage(results,json.path("query").path("more_results_available").asBoolean(false));
        }catch(ResponseStatusException e){throw e;}catch(InterruptedException e){Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Search interrupted");}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Cannot reach Brave Search. Retry later.");}
    }

    /** Fixed provider endpoint; API keys never appear in returned errors or application logs. */
    protected BravePage serpPage(String query,int page,String apiKey) {
        if(apiKey==null||apiKey.isBlank())throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,"Connect your SerpApi API key before searching.");
        try {
            var request=HttpRequest.newBuilder(serpUri(query,page,apiKey)).timeout(Duration.ofSeconds(30)).header("Accept","application/json").GET().build();
            var response=client.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,response.statusCode()==429?"SerpApi quota or rate limit reached. Check your plan and retry later.":"SerpApi rejected the request. Check your API key and plan.");
            return parseSerpPage(response.body());
        }catch(ResponseStatusException e){throw e;}catch(InterruptedException e){Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Search interrupted");}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Cannot reach SerpApi. Retry later.");}
    }
    static URI serpUri(String query,int page,String apiKey) {
        return URI.create("https://serpapi.com/search.json?engine=google&q="+URLEncoder.encode(query,StandardCharsets.UTF_8)+"&start="+(page*10)+"&hl=en&api_key="+URLEncoder.encode(apiKey,StandardCharsets.UTF_8));
    }
    static BravePage parseSerpPage(String body) {
        var json=new tools.jackson.databind.ObjectMapper().readTree(body);
        // SerpApi returns an error for a valid query with no matches as well as provider failures.
        String providerError=json.path("error").asText("");
        if(!providerError.isBlank()) {
            if(providerError.equals("Google hasn't returned any results for this query."))return new BravePage(List.of(),false);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"SerpApi could not complete the search. Check your key, credits, and plan.");
        }
        var results=new ArrayList<GoogleSearchResult>();
        for(var item:json.path("organic_results")) {
            try {var uri=URI.create(item.path("link").asText());if(isWebsite(uri)&&!excluded(uri.getHost()))results.add(new GoogleSearchResult(uri.toString(),item.path("title").asText(),item.path("snippet").asText()));}catch(IllegalArgumentException ignored){}
        }
        // Never follow provider-supplied pagination URLs: rebuild the fixed endpoint instead.
        boolean more=!json.path("serpapi_pagination").path("next").asText("").isBlank();
        return new BravePage(results,more);
    }

    private List<GoogleSearchResult> braveResults(String query, int pages, String braveKey) {
        if (braveKey == null || braveKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Connect your Brave Search API key before searching.");
        try {
            Map<String, GoogleSearchResult> results = new LinkedHashMap<>();
            for (int page=0; page<pages && results.size()<maxResults; page++) {
                var request = HttpRequest.newBuilder(URI.create("https://api.search.brave.com/res/v1/web/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&count=20&offset=" + page)).timeout(Duration.ofSeconds(20)).header("Accept", "application/json").header("X-Subscription-Token", braveKey).GET().build();
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode()!=200) throw new ResponseStatusException(response.statusCode()==429 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY, response.statusCode()==429 ? "Brave Search quota or rate limit reached. Check your Brave plan and retry later." : response.statusCode()==401 || response.statusCode()==403 ? "Brave Search rejected your key. Check the key and active Search plan, then reconnect it." : "Brave Search could not complete the request. Retry later.");
                var json = new tools.jackson.databind.ObjectMapper().readTree(response.body());
                for (var item : json.path("web").path("results")) {
                    try { URI uri = URI.create(item.path("url").asText()); if (!isWebsite(uri)||excluded(uri.getHost())) continue; results.putIfAbsent(siteKey(uri),new GoogleSearchResult(uri.toString(),item.path("title").asText(),item.path("description").asText())); if(results.size()>=maxResults)break; } catch(IllegalArgumentException ignored) { }
                }
                if (!json.path("query").path("more_results_available").asBoolean(false)) break;
            }
            return new ArrayList<>(results.values());
        } catch(ResponseStatusException e) {throw e;} catch(InterruptedException e) {Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Search interrupted");} catch(Exception e) {throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Cannot reach search provider. Retry later.");}
    }

    static boolean isBlocked(String url, String html) {
        String lower = html.toLowerCase(Locale.ROOT);
        return url.contains("/sorry/") || lower.contains("our systems have detected unusual traffic")
                || lower.contains("id=\"captcha-form\"");
    }

    List<GoogleSearchResult> parseSearchResults(String html) {
        Map<String, GoogleSearchResult> results = new LinkedHashMap<>();
        Document doc = Jsoup.parse(html, "https://www.google.com");
        for (Element heading : doc.select("h3, [role=heading], #search a[href]:has(cite), #rso a[href]:has(cite), #rso a[href]:has(.LC20lb)")) {
            Element container = heading.closest("div.MjjYud, div.g, div[data-snhf]");
            Element link = heading.closest("a[href]");
            if (link == null) link = heading.selectFirst("a[href]");
            if (link == null && container != null) link = container.selectFirst("a[href]");
            if (link == null) continue;
            try {
                URI uri = URI.create(link.absUrl("href").replace(" ", "%20"));
                if ("/url".equals(uri.getPath()) && uri.getHost() != null && uri.getHost().matches("(?:www\\.)?google\\.[a-z.]+") && uri.getRawQuery() != null) {
                    for (String part : uri.getRawQuery().split("&")) {
                        if (part.startsWith("q=") || part.startsWith("url=")) {
                            uri = URI.create(URLDecoder.decode(part.substring(part.indexOf('=') + 1), StandardCharsets.UTF_8));
                            break;
                        }
                    }
                }
                if (!isWebsite(uri) || excluded(uri.getHost())) continue;
                URI clean = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery(), null);
                if (container == null) container = link.closest("div.MjjYud, div.g, div[data-snhf]");
                results.putIfAbsent(siteKey(clean), new GoogleSearchResult(clean.toString(), heading.text(),
                        container == null ? "" : container.text()));
                if (results.size() >= maxResults) break;
            } catch (Exception ignored) { /* Ignore malformed search links. */ }
        }
        return new ArrayList<>(results.values());
    }

    static GoogleSearchLead snippetLead(GoogleSearchResult result, int minYear, int maxYear) {
        var notice = java.util.regex.Pattern.compile("(?i)(?:\\bcopyright\\b|©)\\s*(?:©\\s*)?((?:19|20)\\d{2})\\b(?!\\s*[-–—]\\s*\\d)").matcher(result.snippet());
        while (notice.find()) {
            int year = Integer.parseInt(notice.group(1));
            if (year >= minYear && year <= maxYear) {
                return new GoogleSearchLead(result.url(), result.title(), List.of(),
                        notice.group(1), "Google snippet only (unverified): " + result.snippet());
            }
        }
        return null;
    }

    GoogleSearchLead processWebsite(GoogleSearchResult result) { return processWebsite(result,2023,2024); }
    GoogleSearchLead processWebsite(GoogleSearchResult result,int minYear,int maxYear) {
        try {
            Document doc = fetch(URI.create(result.url()));
            if (doc == null) return null;
            String year = findCopyrightYear(doc,minYear,maxYear);
            // Search results can point to a menu or service page with no footer.
            URI landing = URI.create(doc.location());
            if (year == null && landing.getPath() != null && !landing.getPath().matches("/?")) {
                Document home = fetch(landing.resolve("/"));
                if (home != null) { doc = home; year = findCopyrightYear(doc,minYear,maxYear); }
            }
            if (year == null) return null;
            List<String> phones = extractPhoneNumbers(doc);
            if (phones.isEmpty()) {
                Set<String> contacts = new LinkedHashSet<>();
                for (Element link : doc.select("a[href]")) {
                    URI contact;
                    try { contact = URI.create(link.absUrl("href")); }
                    catch (IllegalArgumentException e) { continue; }
                    if (isWebsite(contact) && siteKey(contact).equals(siteKey(URI.create(doc.location())))
                            && (link.text() + " " + contact.getPath()).toLowerCase(Locale.ROOT).contains("contact")) {
                        contacts.add(contact.toString().split("#", 2)[0]);
                    }
                    if (contacts.size() == 2) break;
                }
                for (String contact : contacts) {
                    Document contactDoc = fetch(URI.create(contact));
                    if (contactDoc != null) phones = extractPhoneNumbers(contactDoc);
                    if (!phones.isEmpty()) break;
                }
            }
            return new GoogleSearchLead(doc.location(), result.title().isBlank() ? doc.title() : result.title(),
                    phones, year, result.snippet());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.debug("Skipping {}: {}", result.url(), e.getMessage());
            return null;
        }
    }

    protected Document fetch(URI uri) throws InterruptedException {
        try{return websiteFetcher.fetch(uri);}catch(java.io.IOException|IllegalArgumentException e){return null;}
    }

    static String findCopyrightYear(Document doc) { return findCopyrightYear(doc,2023,2024); }
    static String findCopyrightYear(Document doc,int minYear,int maxYear) {
        // Ignore scripts, comments and template source. Prefer footer notices over article text.
        String text = doc.select("footer, [role=contentinfo], .footer, #footer, .copyright, #copyright").text();
        if (text.isBlank()) text = doc.body() == null ? "" : doc.body().text();
        Matcher matcher = COPYRIGHT.matcher(text);
        int latest = 0;
        while (matcher.find()) {
            int year = Integer.parseInt(matcher.group(1));
            if (matcher.group(2) != null) {
                int end = Integer.parseInt(matcher.group(2));
                int expanded = year / 100 * 100 + end;
                year = expanded < year ? expanded + 100 : expanded;
            }
            latest = Math.max(latest, year);
        }
        return latest >= minYear && latest <= maxYear ? Integer.toString(latest) : null;
    }

    static List<String> extractPhoneNumbers(Document doc) {
        Map<String, String> phones = new LinkedHashMap<>();
        for (Element link : doc.select("a[href]")) {
            String href = link.attr("href");
            if (href.regionMatches(true, 0, "tel:", 0, 4)) {
                addPhone(phones, href.substring(4).split("[;?]", 2)[0]);
            }
            try {
                URI uri = URI.create(link.absUrl("href").replace(" ", "%20"));
                String host = uri.getHost();
                if ("wa.me".equalsIgnoreCase(host)) addPhone(phones, "+" + uri.getPath().substring(1));
                if ("api.whatsapp.com".equalsIgnoreCase(host) && uri.getRawQuery() != null) {
                    for (String part : uri.getRawQuery().split("&")) {
                        if (part.startsWith("phone=")) addPhone(phones, "+" + URLDecoder.decode(part.substring(6), StandardCharsets.UTF_8));
                    }
                }
            } catch (Exception ignored) { }
        }
        for (Element data : doc.select("[itemprop=telephone]")) {
            addPhone(phones, data.hasAttr("content") ? data.attr("content") : data.text());
        }
        for (Element script : doc.select("script[type=application/ld+json]")) {
            Matcher matcher = JSON_PHONE.matcher(script.data());
            while (matcher.find()) addPhone(phones, matcher.group(1));
        }
        if (doc.body() != null) {
            Matcher matcher = PHONE.matcher(doc.body().text());
            while (matcher.find()) addPhone(phones, matcher.group());
        }
        return phones.values().stream().limit(5).toList();
    }

    private static void addPhone(Map<String, String> phones, String value) {
        String digits = value.replaceAll("\\D", "");
        // Conservative public phone extraction, not a claim that a number is mobile or active.
        if (digits.length() < 10 || digits.length() > 15 || digits.matches("(\\d)\\1+")) return;
        phones.putIfAbsent(digits, (value.strip().startsWith("+") ? "+" : "") + digits);
    }

    private static boolean isWebsite(URI uri) {
        return uri.getHost() != null && uri.getUserInfo() == null
                && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()));
    }

    private static boolean excluded(String host) {
        String lower = host.toLowerCase(Locale.ROOT);
        return lower.matches("(?:.*\\.)?google\\.[a-z.]+") || EXCLUDED.stream()
                .anyMatch(domain -> lower.equals(domain) || lower.endsWith("." + domain));
    }

    private static String siteKey(URI uri) {
        return uri.getHost().toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "")
                + (uri.getPort() < 0 ? "" : ":" + uri.getPort());
    }

    protected record GoogleSearchResult(String url, String title, String snippet) { }
}
