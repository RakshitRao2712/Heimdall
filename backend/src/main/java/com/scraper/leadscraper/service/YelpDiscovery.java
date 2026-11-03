package com.scraper.leadscraper.service;

import com.scraper.leadscraper.history.SearchHistory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class YelpDiscovery {
    private static final int PAGE_SIZE = 50;
    private static final int SEARCH_CAP = 240;
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final ObjectMapper JSON = new ObjectMapper();

    private YelpDiscovery() {}

    public static SearchProgress.Outcome<SearchHistory.Lead> search(String query, String key,
            BooleanSupplier cancelled, Consumer<SearchProgress> progress) {
        String[] parts = query.strip().split("(?i)\\s+in\\s+", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()
                || parts[0].length() > 250 || parts[1].length() > 250) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a business type and location, such as restaurants in Boston.");
        }
        var leads = new LinkedHashMap<String, SearchHistory.Lead>();
        int requests = 0;
        int checked = 0;
        String reason = "Reached the end of the listings Yelp returned.";
        try {
            for (int offset = 0; offset < SEARCH_CAP && !cancelled.getAsBoolean();) {
                int limit = Math.min(PAGE_SIZE, SEARCH_CAP - offset);
                var uri = URI.create("https://api.yelp.com/v3/businesses/search?term=" + encode(parts[0])
                        + "&location=" + encode(parts[1]) + "&limit=" + limit + "&offset=" + offset);
                var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20))
                        .header("Authorization", "Bearer " + key).header("Accept", "application/json").build();
                progress.accept(new SearchProgress(SEARCH_CAP, checked, ++requests, leads.size(), "searching", "Reading Yelp listings."));
                var response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw switch (response.statusCode()) {
                        case 401, 403 -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yelp rejected the key or this plan does not include Business Search.");
                        case 429 -> new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Yelp's request limit was reached.");
                        default -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Yelp could not complete this search. Check the location and try again.");
                    };
                }
                var payload = JSON.readTree(response.body());
                var businesses = payload.path("businesses");
                if (!businesses.isArray()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Yelp returned an unexpected search response.");
                for (var business : businesses) {
                    checked++;
                    String id = business.path("id").asText("");
                    String name = business.path("name").asText("");
                    if (id.isBlank() || name.isBlank()) continue;
                    String phone = business.path("phone").asText("");
                    var address = new ArrayList<String>();
                    business.path("location").path("display_address").forEach(line -> address.add(line.asText()));
                    var socials = new LinkedHashSet<String>();
                    for (String field : List.of("facebook_url", "instagram_url", "twitter_url", "linkedin_url", "tiktok_url")) {
                        addSocial(socials, business.path(field).asText(""));
                    }
                    business.path("social_links").forEach(link -> addSocial(socials, link.isTextual() ? link.asText() : link.path("url").asText("")));
                    leads.putIfAbsent(id, new SearchHistory.Lead(name, business.path("url").asText(""),
                            phone.isBlank() ? List.of() : List.of(phone), String.join(", ", address), "",
                            business.path("review_count").asInt(), List.copyOf(socials)));
                }
                offset += businesses.size();
                if (offset >= SEARCH_CAP) {
                    reason = "Reached Yelp's 240-listing search cap.";
                    break;
                }
                if (businesses.size() < limit || offset >= payload.path("total").asInt(Integer.MAX_VALUE)) break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            reason = "Yelp search interrupted.";
        } catch (IOException e) {
            if (leads.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not reach Yelp.");
            reason = "Connection to Yelp ended early. Showing listings already received.";
        } catch (ResponseStatusException e) {
            if (leads.isEmpty()) throw e;
            reason = e.getReason() + " Showing listings already received.";
        }
        if (cancelled.getAsBoolean()) reason = "Yelp search cancelled.";
        var state = new SearchProgress(SEARCH_CAP, checked, requests, leads.size(), "complete", reason);
        progress.accept(state);
        return new SearchProgress.Outcome<>(List.copyOf(leads.values()), state);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value.strip(), StandardCharsets.UTF_8);
    }

    private static void addSocial(Set<String> links, String value) {
        try {
            var uri = URI.create(value);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getUserInfo() != null) return;
            for (String domain : List.of("facebook.com", "instagram.com", "twitter.com", "x.com", "linkedin.com", "tiktok.com")) {
                if (host.equalsIgnoreCase(domain) || host.toLowerCase(Locale.ROOT).endsWith("." + domain)) {
                    links.add(value);
                    return;
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Ignore malformed provider links.
        }
    }
}
