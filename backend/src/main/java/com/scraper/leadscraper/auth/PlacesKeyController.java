package com.scraper.leadscraper.auth;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

/** Keys exist only in the signed-in session and are never returned or written to disk. */
@RestController
@RequestMapping("/api/workspace/places-key")
public class PlacesKeyController {
    private static final String ATTRIBUTE = PlacesKeyController.class.getName() + ".key";
    public record KeyInput(String apiKey) { }
    public static String requireKey(HttpSession session) {
        Object value = session.getAttribute(ATTRIBUTE);
        if (!(value instanceof String key) || key.isBlank()) throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Add your Google Places API key before searching.");
        return key;
    }
    @GetMapping public Map<String, Boolean> status(HttpSession session) { return Map.of("configured", session.getAttribute(ATTRIBUTE) instanceof String); }
    @PostMapping public Map<String, Boolean> save(@RequestBody KeyInput input, HttpSession session) {
        String key = input.apiKey() == null ? "" : input.apiKey().trim();
        if (!key.matches("[A-Za-z0-9_-]{20,200}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid Google Places API key.");
        session.setAttribute(ATTRIBUTE, key);
        return Map.of("configured", true);
    }
    @DeleteMapping public Map<String, Boolean> remove(HttpSession session) { session.removeAttribute(ATTRIBUTE); return Map.of("configured", false); }
}
