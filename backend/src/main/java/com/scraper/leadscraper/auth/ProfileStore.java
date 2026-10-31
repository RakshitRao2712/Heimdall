package com.scraper.leadscraper.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.net.URI;
import java.util.HexFormat;
import java.util.Locale;
import java.text.Normalizer;

@Service
public class ProfileStore {
    public record Profile(String firstName, String lastName, String username, String website, boolean completed) { }
    private final Path directory;
    private final ObjectMapper mapper;
    public ProfileStore(@Value("${heimdall.profiles.directory:.data/profiles}") String directory, ObjectMapper mapper) {
        this.directory = Path.of(directory); this.mapper = mapper;
    }
    private Path path(String email) {
        try { return directory.resolve(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(email.getBytes(StandardCharsets.UTF_8))) + ".json"); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    public synchronized Profile get(String email) {
        try { return Files.exists(path(email)) ? mapper.readValue(Files.readAllBytes(path(email)), Profile.class) : new Profile("", "", "", "", false); }
        catch (Exception e) { throw new IllegalStateException("Cannot load your profile", e); }
    }
    private String text(String value, String field, boolean required, int max) {
        String clean = value == null ? "" : value.strip();
        if ((required && clean.isEmpty()) || clean.length() > max || clean.chars().anyMatch(Character::isISOControl))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + (required ? " is required and must be " : " must be ") + "at most " + max + " characters.");
        return clean;
    }
    private String displayName(String input) {
        String normalized = Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFKC).replaceAll("\\p{Cf}", "");
        return text(normalized, "Display name", true, 60).replaceAll("[\\p{Z}\\s]+", " ").strip();
    }
    private String nameKey(String input) { return displayName(input).toLowerCase(Locale.ROOT); }
    public synchronized boolean available(String email, String input) {
        String key = nameKey(input);
        if (!Files.exists(directory)) return true;
        try (var paths = Files.list(directory)) {
            for (Path file : paths.filter(value -> value.getFileName().toString().matches("[0-9a-f]{64}\\.json")).toList()) {
                if (file.equals(path(email))) continue;
                Profile other = mapper.readValue(Files.readAllBytes(file), Profile.class);
                if (other.username() != null && !other.username().isBlank() && nameKey(other.username()).equals(key)) return false;
            }
            return true;
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot check display name availability", e); }
    }
    public synchronized Profile save(String email, Profile input) {
        String first = text(input.firstName(), "First name", true, 60);
        String last = text(input.lastName(), "Last name", false, 60);
        String username = displayName(input.username());
        if (!available(email, username)) throw new ResponseStatusException(HttpStatus.CONFLICT, "This display name is already taken. Choose a different name.");
        String website = text(input.website(), "Website", false, 250).replaceFirst("(?i)^https?://", "");
        if (!website.isEmpty()) {
            try { URI uri = new URI("https://" + website); if (uri.getHost() == null || !uri.getHost().contains(".") || uri.getUserInfo() != null) throw new Exception(); }
            catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid website, such as example.com."); }
        }
        Profile profile = new Profile(first, last, username, website, true);
        Path temporary = null;
        try {
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, "profile-", ".tmp");
            try { Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rw-------")); } catch (UnsupportedOperationException ignored) { }
            Files.write(temporary, mapper.writeValueAsBytes(profile));
            Files.move(temporary, path(email), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return profile;
        } catch (Exception e) { throw new IllegalStateException("Cannot save your profile", e); }
        finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (Exception ignored) { } }
    }
}
