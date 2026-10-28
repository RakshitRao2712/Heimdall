package com.scraper.leadscraper.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountStoreTest {
    @TempDir Path directory;
    @Test void persistsHashesRejectsDuplicateAccountsAndChecksPasswords() throws Exception {
        Path file = directory.resolve("accounts.properties");
        var encoder = new BCryptPasswordEncoder(4);
        var store = new AccountStore(file.toString());
        String hash = encoder.encode("local-test-password");
        assertTrue(store.create("test@example.com", hash));
        assertFalse(store.create("test@example.com", encoder.encode("replacement-password")));
        var reopened = new AccountStore(file.toString());
        assertTrue(encoder.matches("local-test-password", reopened.hash("test@example.com")));
        assertFalse(encoder.matches("wrong-password", reopened.hash("test@example.com")));
        assertFalse(Files.readString(file).contains("local-test-password"));
        assertNull(reopened.hash("missing@example.com"));
    }
}
