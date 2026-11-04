package com.scraper.leadscraper.service;

import com.scraper.leadscraper.history.SearchHistory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.*;

public final class FoursquareDiscovery {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int LIMIT = 50;

    private FoursquareDiscovery() {}

    public static SearchProgress.Outcome<SearchHistory.Lead> search(String query, String key, int target,
            BooleanSupplier cancelled, Consumer<SearchProgress> progress) {
        if (target < 1 || target > LIMIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a lead target between 1 and 50.");
        }
        String[] parts = query.strip().split("(?i)\\s+in\\s+", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank() || query.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a business type and location, such as restaurants in Boston.");
        }
        if (cancelled.getAsBoolean()) return finish(target, List.of(), 0, 0, "Search cancelled.", progress);
        var uri = URI.create("https://places-api.foursquare.com/places/search?query=" + encode(parts[0])
                + "&near=" + encode(parts[1]) + "&limit=" + target
                + "&fields=fsq_place_id,name,location,tel,website,social_media");
        var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + key)
                .header("X-Places-Api-Version", "2025-06-17")
                .header("Accept", "application/json").build();
        progress.accept(new SearchProgress(target, 0, 1, 0, "searching", "Reading Foursquare listings."));
        try {
            var response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (cancelled.getAsBoolean()) return finish(target, List.of(), 0, 1, "Search cancelled.", progress);
            switch (response.statusCode()) {
                case 200 -> { }
                case 401, 403 -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Foursquare rejected the API key or plan.");
                case 429 -> throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Foursquare's request limit was reached.");
                default -> throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Foursquare could not complete this search. Check the location and try again.");
            }
            var rows = JSON.readTree(response.body()).path("results");
            if (!rows.isArray()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Foursquare returned an unexpected response.");
            var leads = new LinkedHashMap<String, SearchHistory.Lead>();
            for (var row : rows) {
                String id = row.path("fsq_place_id").asText("");
                String name = row.path("name").asText("");
                if (id.isBlank() || name.isBlank()) continue;
                String phone = row.path("tel").asText("");
                var social = row.path("social_media");
                var links = new ArrayList<String>();
                addProfile(links, "https://www.facebook.com/", social.path("facebook_id").asText(""));
                addProfile(links, "https://www.instagram.com/", social.path("instagram").asText(""));
                addProfile(links, "https://x.com/", social.path("twitter").asText(""));
                leads.putIfAbsent(id, new SearchHistory.Lead(name, row.path("website").asText(""),
                        phone.isBlank() ? List.of() : List.of(phone),
                        row.path("location").path("formatted_address").asText(""), "", null, links));
            }
            return finish(target, List.copyOf(leads.values()), rows.size(), 1,
                    leads.size() >= target ? "Lead target reached." : "Foursquare search complete.", progress);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return finish(target, List.of(), 0, 1, "Search interrupted.", progress);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not read results from Foursquare.");
        }
    }

    private static SearchProgress.Outcome<SearchHistory.Lead> finish(int target, List<SearchHistory.Lead> leads,
            int checked, int requests, String reason, Consumer<SearchProgress> progress) {
        var state = new SearchProgress(target, checked, requests, leads.size(), "complete", reason);
        progress.accept(state);
        return new SearchProgress.Outcome<>(leads, state);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value.strip(), StandardCharsets.UTF_8);
    }

    private static void addProfile(List<String> links, String base, String handle) {
        String clean = handle.strip().replaceFirst("^@", "");
        if (clean.matches("[A-Za-z0-9_][A-Za-z0-9_.-]{0,99}")) links.add(base + clean);
    }
}
