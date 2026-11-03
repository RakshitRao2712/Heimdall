package com.scraper.leadscraper.history;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class SearchHistory {
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record Lead(String name, String website, List<String> phones, String address, String year, Integer reviews, List<String> socials) {
        public Lead(String name, String website, List<String> phones, String address, String year, Integer reviews) {
            this(name, website, phones, address, year, reviews, List.of());
        }
        public Lead {
            socials = socials == null ? List.of() : List.copyOf(socials);
        }
    }
    public record Run(String id, String method, String query, String createdAt, long elapsedMs, List<Lead> leads, com.scraper.leadscraper.service.SearchProgress progress) {
        public Run(String id,String method,String query,String createdAt,long elapsedMs,List<Lead> leads) {this(id,method,query,createdAt,elapsedMs,leads,null);}
    }
    private final Path directory;
    private final ObjectMapper mapper;
    public SearchHistory(@Value("${heimdall.history.directory:.data/searches}") String directory, ObjectMapper mapper) {
        this.directory = Path.of(directory); this.mapper = mapper;
    }
    private Path file(String email) {
        try { return directory.resolve(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(email.getBytes(StandardCharsets.UTF_8))) + ".json"); }
        catch (Exception e) { throw new IllegalStateException("Cannot identify search history", e); }
    }
    public synchronized List<Run> list(String email) {
        Path file = file(email);
        if (!Files.exists(file)) return List.of();
        try { return List.of(mapper.readValue(Files.readAllBytes(file), Run[].class)); }
        catch (Exception e) { throw new IllegalStateException("Cannot read search history", e); }
    }
    public synchronized Run save(String email, String method, String query, long elapsedMs, List<Lead> leads) {
        return save(email,method,query,elapsedMs,leads,null);
    }
    public synchronized Run save(String email,String method,String query,long elapsedMs,List<Lead> leads,com.scraper.leadscraper.service.SearchProgress progress) {
        Run run = new Run(UUID.randomUUID().toString(), method, query.trim(), Instant.now().toString(), elapsedMs, List.copyOf(leads),progress);
        List<Run> runs = new ArrayList<>(); runs.add(run); runs.addAll(list(email));
        if (runs.size() > 50) runs = new ArrayList<>(runs.subList(0, 50));
        write(email,runs);return run;
    }
    private void write(String email,List<Run> runs) {
        Path temporary = null;
        try {
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, "search-", ".tmp");
            try { Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rw-------")); } catch (UnsupportedOperationException ignored) { }
            Files.write(temporary, mapper.writeValueAsBytes(runs));
            Files.move(temporary, file(email), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) { throw new IllegalStateException("Cannot save search history", e); }
        finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (Exception ignored) { } }
    }
}
