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

    public ScraperService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<PlaceResponse.Place> getLeadsWithoutWebsite(String query, String apiKey) {
        if (query == null || query.isBlank() || query.length() > 500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supply a query of 1–500 characters");
        if (apiKey == null || apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Add your Google Places API key before searching.");

        try {
            String jsonPayload = objectMapper.writeValueAsString(Map.of("textQuery", query.trim()));
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://places.googleapis.com/v1/places:searchText"))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask",
                            "places.displayName,places.websiteUri,places.formattedAddress,places.nationalPhoneNumber,places.businessStatus,places.userRatingCount")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Google Places API response status: {}", response.statusCode());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = response.statusCode() == 403 || response.statusCode() == 401
                    ? "Google Places rejected the API key. Check API access, billing, and key restrictions on the backend."
                    : response.statusCode() == 429 ? "Google Places quota exceeded. Try again later."
                    : "Google Places could not complete the search. Try again later.";
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
            }

            PlaceResponse placeResponse = objectMapper.readValue(response.body(), PlaceResponse.class);

            if (placeResponse.places() == null)
                return List.of();

            // Stream and filter for missing websites, operational status, and recently
            // added (proxy: low review count)
            return placeResponse.places().stream()
                    .filter(place -> place.websiteUri() == null || place.websiteUri().isEmpty())
                    .filter(place -> "OPERATIONAL".equals(place.businessStatus()))
                    .filter(place -> place.userRatingCount() == null
                            || place.userRatingCount() <= maxReviewsForNewBusiness)
                    .toList();

        } catch (ResponseStatusException e) { throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Search interrupted", e);
        } catch (Exception e) {
            log.error("Error fetching leads from Google Places API", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cannot reach Google Places. Check backend network access and retry.", e);
        }
    }
}
