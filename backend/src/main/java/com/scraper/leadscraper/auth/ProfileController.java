package com.scraper.leadscraper.auth;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/workspace/profile")
public class ProfileController {
    public record Input(String firstName, String lastName, String username, String website) { }
    private final ProfileStore profiles;
    public ProfileController(ProfileStore profiles) { this.profiles = profiles; }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> error(org.springframework.web.server.ResponseStatusException error) {
        return org.springframework.http.ResponseEntity.status(error.getStatusCode()).body(java.util.Map.of("detail", error.getReason() == null ? "Could not update your profile." : error.getReason()));
    }
    @GetMapping("/availability") public java.util.Map<String, Boolean> available(@RequestParam String username, Principal user) { return java.util.Map.of("available", profiles.available(user.getName(), username)); }
    @GetMapping public ProfileStore.Profile get(Principal user) { return profiles.get(user.getName()); }
    @PostMapping public ProfileStore.Profile save(@RequestBody Input input, Principal user) { return profiles.save(user.getName(), new ProfileStore.Profile(input.firstName(), input.lastName(), input.username(), input.website(), false)); }
}
