package com.scraper.leadscraper.auth;

import com.scraper.leadscraper.storage.PostgresDocuments;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;

/** Single-instance local account store; stores hashes only, never raw passwords. */
@Service
public class AccountStore {
    private PostgresDocuments documents;

    @Autowired(required = false)
    void usePostgres(PostgresDocuments documents) {
        this.documents = documents;
    }

    private final Path file;
    public AccountStore(@Value("${heimdall.auth.file:.data/accounts.properties}") String file) { this.file = Path.of(file).toAbsolutePath(); }
    private Properties read() throws IOException {
        if (documents != null) return documents.readProperties("accounts", "passwords");
        Properties accounts = new Properties();
        if (Files.exists(file)) try (InputStream input = Files.newInputStream(file)) { accounts.load(input); }
        return accounts;
    }
    public synchronized String hash(String email) throws IOException { return read().getProperty(email); }
    public synchronized boolean create(String email, String hash) throws IOException {
        Properties accounts = read();
        if (accounts.containsKey(email)) return false;
        accounts.setProperty(email, hash);
        if (documents != null) {
            documents.writeProperties("accounts", "passwords", accounts);
            return true;
        }
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "accounts-", ".tmp");
        try {
            try { Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rw-------")); } catch (UnsupportedOperationException ignored) { }
            try (OutputStream output = Files.newOutputStream(temporary)) { accounts.store(output, "Heimdall password hashes"); }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
        return true;
    }
}
