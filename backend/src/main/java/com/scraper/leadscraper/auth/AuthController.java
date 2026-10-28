package com.scraper.leadscraper.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record Credentials(String email, String password, boolean rememberMe) { }
    private final AccountStore accounts;
    private final PasswordEncoder encoder;
    private final HttpSessionSecurityContextRepository contexts;
    private final String dummyHash;
    private record Attempts(long since, int count) { }
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    public AuthController(AccountStore accounts, PasswordEncoder encoder, HttpSessionSecurityContextRepository contexts) {
        this.accounts = accounts; this.encoder = encoder; this.contexts = contexts;
        dummyHash = encoder.encode(UUID.randomUUID().toString());
    }
    @GetMapping("/csrf") public Map<String, String> csrf(CsrfToken token) { return Map.of("token", token.getToken(), "headerName", token.getHeaderName()); }
    @GetMapping("/me") public Map<String, String> me(Principal principal) { return Map.of("email", principal.getName()); }
    private String validate(Credentials credentials) {
        String email = credentials.email() == null ? "" : credentials.email().trim().toLowerCase(Locale.ROOT);
        String password = credentials.password();
        if (email.length() > 254 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || password == null || password.length() < 10 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a valid email and a password of at least 10 characters (up to 72 UTF-8 bytes)");
        return email;
    }
    private void throttle(HttpServletRequest request) {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(e -> now - e.getValue().since() > 900_000);
        if (attempts.size() > 10_000) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Try again later");
        Attempts next = attempts.compute(request.getRemoteAddr(), (key, previous) -> previous == null ? new Attempts(now, 1) : new Attempts(previous.since(), previous.count() + 1));
        if (next.count() > 20) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts; try again in 15 minutes");
    }
    private Map<String, String> session(String email, boolean remember, HttpServletRequest request, HttpServletResponse response) {
        var existing = request.getSession(false); if (existing != null) existing.invalidate();
        var session = request.getSession(true); session.setMaxInactiveInterval(remember ? 604800 : 28800);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(email, null, List.of()));
        SecurityContextHolder.setContext(context); contexts.saveContext(context, request, response);
        return Map.of("email", email);
    }
    @PostMapping("/register") public Map<String, String> register(@RequestBody Credentials credentials, HttpServletRequest request, HttpServletResponse response) throws IOException {
        throttle(request); String email = validate(credentials);
        if (!accounts.create(email, encoder.encode(credentials.password()))) throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create this account; try signing in");
        return session(email, credentials.rememberMe(), request, response);
    }
    @PostMapping("/login") public Map<String, String> login(@RequestBody Credentials credentials, HttpServletRequest request, HttpServletResponse response) throws IOException {
        throttle(request); String email = validate(credentials); String hash = accounts.hash(email);
        boolean matches = encoder.matches(credentials.password(), hash == null ? dummyHash : hash);
        if (!matches || hash == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect");
        return session(email, credentials.rememberMe(), request, response);
    }
}
