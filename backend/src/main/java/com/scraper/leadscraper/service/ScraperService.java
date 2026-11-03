package com.scraper.leadscraper.service;

import tools.jackson.databind.ObjectMapper;
import com.scraper.leadscraper.dto.PlaceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

//commen
@Service
public class ScraperService {

    private static final Logger log = LoggerFactory.getLogger(ScraperService.class);

    @Value("${max.reviews.for.new.business:5}")
    private int maxReviewsForNewBusiness;

    private final ObjectMapper objectMapper;
    private final HttpClient client;

    @org.springframework.beans.factory.annotation.Autowired
    public ScraperService(ObjectMapper objectMapper) { this(objectMapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()); }
    ScraperService(ObjectMapper objectMapper, HttpClient client) { this.objectMapper = objectMapper; this.client = client; maxReviewsForNewBusiness = 5; }
    @jakarta.annotation.PreDestroy public void close() { client.close(); }

    public List<PlaceResponse.Place> getLeadsWithoutWebsite(String query, String apiKey) {
        return search(query, apiKey, 10, p -> {}).leads();
    }
    public SearchProgress.Outcome<PlaceResponse.Place> search(String query, String apiKey, int target, java.util.function.Consumer<SearchProgress> progress) {
        return searchInternal(query,apiKey,target,3,false,()->false,progress);
    }
    public SearchProgress.Outcome<PlaceResponse.Place> searchUntilTarget(String query,String apiKey,int target,int maxRequests,java.util.function.BooleanSupplier cancelled,java.util.function.Consumer<SearchProgress> progress) {
        return searchInternal(query,apiKey,target,maxRequests,true,cancelled,progress);
    }
    private SearchProgress.Outcome<PlaceResponse.Place> searchInternal(String query,String apiKey,int target,int maxRequests,boolean expand,java.util.function.BooleanSupplier cancelled,java.util.function.Consumer<SearchProgress> progress) {
        if(maxRequests<1||maxRequests>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a request budget between 1 and 100");
        if (query == null || query.isBlank() || query.length() > 500 || !List.of(10,25,50).contains(target)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supply a query and a target of 10, 25, or 50");
        if (apiKey == null || apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Add your Google Places API key before searching.");
        var leads = new java.util.LinkedHashMap<String,PlaceResponse.Place>();
        var seenTokens = new java.util.HashSet<String>(); String token = ""; int checked=0, requests=0; String reason="Provider results exhausted";
        try {
            var queries=expand?SearchQueries.variants(query):List.of(query.trim());
            searchLoop: for(String expandedQuery:queries) {
            token="";seenTokens.clear();
            for (int page=0;page<3;page++) {
                if(cancelled.getAsBoolean()){reason="Cancelled; partial results retained";break searchLoop;}
                if(requests>=maxRequests){reason="Your API request budget was reached. Increase it to continue searching.";break searchLoop;}
                var payload = new java.util.LinkedHashMap<String,Object>(); payload.put("textQuery",expandedQuery); payload.put("pageSize",20); if (!token.isBlank()) payload.put("pageToken",token);
                requests++; progress.accept(new SearchProgress(target,checked,requests,leads.size(),"searching","Searching: " + expandedQuery + " · request " + requests));
                var request = HttpRequest.newBuilder(URI.create("https://places.googleapis.com/v1/places:searchText")).timeout(Duration.ofSeconds(45))
                    .header("Content-Type","application/json").header("X-Goog-Api-Key",apiKey)
                    .header("X-Goog-FieldMask","nextPageToken,places.id,places.displayName,places.websiteUri,places.formattedAddress,places.nationalPhoneNumber,places.businessStatus,places.userRatingCount")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload))).build();
                var response=client.send(request,HttpResponse.BodyHandlers.ofString());
                if(response.statusCode()<200||response.statusCode()>=300) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,response.statusCode()==429?"Google Places quota exceeded. Try again later.":response.statusCode()<500?"Google Places rejected the API key. Check API access, billing, and key restrictions.":"Google Places could not complete the search. Try again later.");
                var json=objectMapper.readTree(response.body());
                for(var node:json.path("places")) {
                    if(node.isNull()) continue; checked++;
                    var place=objectMapper.treeToValue(node,PlaceResponse.Place.class);
                    if ((place.websiteUri()==null||place.websiteUri().isBlank()) && "OPERATIONAL".equals(place.businessStatus()) && (maxReviewsForNewBusiness<0||place.userRatingCount()==null||place.userRatingCount()<=maxReviewsForNewBusiness)) {
                        String id=node.path("id").asText(); if(id.isBlank()) id=((place.displayName()==null?"":place.displayName().text())+"|"+place.formattedAddress()+"|"+place.nationalPhoneNumber()).toLowerCase(java.util.Locale.ROOT);
                        leads.putIfAbsent(id,place); if(leads.size()>=target) break;
                    }
                }
                progress.accept(new SearchProgress(target,checked,requests,leads.size(),"searching","Filtering businesses without websites"));
                if(leads.size()>=target) {reason="Target reached";break searchLoop;}
                token=json.path("nextPageToken").asText(""); if(token.isBlank()) {reason="Provider results exhausted";break;} if(!seenTokens.add(token)) {reason="Provider returned a repeated page token; stopped to avoid duplicate charges.";break;}
                reason="Google Places limit reached: 3 pages / up to 60 candidates. Try another location or category.";
            }
            }
            if(expand && leads.size()<target && requests<maxRequests && !cancelled.getAsBoolean()) reason="All available related searches exhausted; not enough qualifying businesses were found.";
        } catch(Exception e) {
            if(e instanceof InterruptedException) Thread.currentThread().interrupt();
            if(leads.isEmpty()) { if(e instanceof ResponseStatusException status) throw status; throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Cannot reach Google Places. Check network access and retry."); }
            reason="Stopped early after a provider or network error; partial results retained.";
        }
        var state=new SearchProgress(target,checked,requests,leads.size(),"complete",reason);progress.accept(state);
        return new SearchProgress.Outcome<>(List.copyOf(leads.values()),state);
    }
}
