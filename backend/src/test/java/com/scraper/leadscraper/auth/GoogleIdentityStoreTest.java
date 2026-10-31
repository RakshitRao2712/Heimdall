package com.scraper.leadscraper.auth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class GoogleIdentityStoreTest {
    @TempDir Path directory;
    private AccountStore accounts() { return new AccountStore(directory.resolve("accounts.properties").toString()); }
    private GoogleIdentityStore store() { return new GoogleIdentityStore(directory.resolve("identities.properties").toString(), accounts(), new BCryptPasswordEncoder(4)); }
    @Test void verifiedAccountPersistsAndStableSubjectSurvivesEmailChange() throws Exception {
        assertEquals("alex@gmail.com", store().signIn("subject-1", "Alex@gmail.com", true, null));
        assertNotNull(accounts().hash("alex@gmail.com"));
        assertEquals("alex@gmail.com", store().signIn("subject-1", "new@gmail.com", true, null));
        assertNull(accounts().hash("new@gmail.com"));
    }
    @Test void unverifiedIdentityCannotCreateAccount() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> store().signIn("subject", "a@gmail.com", false, null));
        assertNull(accounts().hash("a@gmail.com"));
    }
    @Test void onlyAuthoritativeGoogleEmailCanLinkExistingPasswordAccount() throws Exception {
        accounts().create("existing@example.com", "existing-hash");
        assertThrows(IllegalArgumentException.class, () -> store().signIn("untrusted", "existing@example.com", true, null));
        assertEquals("existing@example.com", store().signIn("workspace", "existing@example.com", true, "example.com"));
        assertEquals("existing-hash", accounts().hash("existing@example.com"));
        accounts().create("existing@gmail.com", "gmail-hash");
        assertEquals("existing@gmail.com", store().signIn("gmail", "existing@gmail.com", true, null));
        assertEquals("gmail-hash", accounts().hash("existing@gmail.com"));
    }
}
