package com.scraper.leadscraper.auth;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
public class GoogleAuthController {
    private final Environment env;
    public GoogleAuthController(Environment env) { this.env = env; }
    @GetMapping("/api/auth/google/config") public Map<String, Boolean> config() {
        return Map.of("enabled", !env.getProperty("GOOGLE_CLIENT_ID", "").isBlank() && !env.getProperty("GOOGLE_CLIENT_SECRET", "").isBlank());
    }
}
