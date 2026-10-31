package com.scraper.leadscraper.auth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
@Service
public class GoogleIdentityStore {
    private final Path file;
    private final AccountStore accounts;
    private final PasswordEncoder encoder;
    public GoogleIdentityStore(@Value("${heimdall.google.identities:.data/google-identities.properties}") String file, AccountStore accounts, PasswordEncoder encoder) {
        this.file = Path.of(file).toAbsolutePath(); this.accounts = accounts; this.encoder = encoder;
    }
    public synchronized String signIn(String subject, String email, boolean verified, String hostedDomain) throws IOException {
        if (subject == null || subject.isBlank() || !verified || email == null || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Verified Google identity required");
        Properties identities = new Properties();
        if (Files.exists(file)) try (var input = Files.newInputStream(file)) { identities.load(input); }
        String bound = identities.getProperty(subject);
        if (bound != null) return bound;
        email = email.trim().toLowerCase(Locale.ROOT);
        // Only authoritative Google email identities can link existing local accounts.
        if (accounts.hash(email) != null && !email.endsWith("@gmail.com") && (hostedDomain == null || hostedDomain.isBlank())) throw new IllegalArgumentException("Use your password to sign in");
        accounts.create(email, encoder.encode(UUID.randomUUID().toString()));
        identities.setProperty(subject, email);
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "google-identities-", ".tmp");
        try {
            try { Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rw-------")); } catch (UnsupportedOperationException ignored) { }
            try (var output = Files.newOutputStream(temporary)) { identities.store(output, "Google subject to Heimdall account"); }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
        return email;
    }
}
