package com.scraper.leadscraper.auth;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class RegistrationOtp {
    public record Challenge(String challengeId, String email, int expiresIn, int resendAfter) { }
    public record Verified(String email, boolean rememberMe) { }
    private record Pending(String email, String passwordHash, boolean remember, byte[] codeHash, long expiresAt, int attempts) { }
    private record Sends(long since, long last, int count) { }
    private final AccountStore accounts; private final OtpMailer mailer; private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Pending> pending = new HashMap<>();
    private final Map<String, Sends> sends = new HashMap<>();
    @org.springframework.beans.factory.annotation.Autowired
    public RegistrationOtp(AccountStore accounts, OtpMailer mailer) { this(accounts, mailer, Clock.systemUTC()); }
    RegistrationOtp(AccountStore accounts, OtpMailer mailer, Clock clock) { this.accounts = accounts; this.mailer = mailer; this.clock = clock; }
    private byte[] hash(String id, String code) {
        try { return MessageDigest.getInstance("SHA-256").digest((id + ":" + code).getBytes(StandardCharsets.UTF_8)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST, "This code is invalid or expired. Request a new code if needed."); }
    private void cleanup(long now) {
        pending.entrySet().removeIf(e -> e.getValue().expiresAt <= now);
        sends.entrySet().removeIf(e -> now - e.getValue().since >= 3_600_000);
    }
    public Challenge start(String email, String passwordHash, boolean remember) {
        return issue(email, passwordHash, remember, null);
    }
    public Challenge resend(String id) {
        Pending previous;
        synchronized (this) { cleanup(clock.millis()); previous = pending.get(id); if (previous == null) throw invalid(); }
        return issue(previous.email, previous.passwordHash, previous.remember, id);
    }
    private Challenge issue(String email, String passwordHash, boolean remember, String oldId) {
        String id = UUID.randomUUID().toString(); String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        synchronized (this) {
            long now = clock.millis(); cleanup(now);
            Sends previous = sends.get(email);
            if (previous != null && (now - previous.last < 60_000 || previous.count >= 5))
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, previous.count >= 5 ? "Too many verification emails. Try again in an hour." : "Wait 60 seconds before requesting another code.");
            if (pending.size() >= 10_000 || sends.size() >= 10_000) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Registration is busy. Please try again later.");
            sends.put(email, new Sends(previous == null ? now : previous.since, now, previous == null ? 1 : previous.count + 1));
        }
        // Never publish a challenge or claim success unless the provider accepted the email.
        mailer.send(email, code);
        synchronized (this) {
            if (oldId != null && !pending.containsKey(oldId)) throw invalid();
            pending.entrySet().removeIf(e -> e.getValue().email.equals(email));
            pending.put(id, new Pending(email, passwordHash, remember, hash(id, code), clock.millis() + 600_000, 0));
        }
        return new Challenge(id, email, 600, 60);
    }
    public synchronized Verified verify(String id, String code) throws java.io.IOException {
        cleanup(clock.millis()); Pending entry = pending.get(id);
        if (entry == null) throw invalid();
        if (code == null || !code.matches("[0-9]{6}") || !MessageDigest.isEqual(entry.codeHash, hash(id, code))) {
            int attempts = entry.attempts + 1;
            if (attempts >= 5) { pending.remove(id); throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many incorrect codes. Start registration again."); }
            pending.put(id, new Pending(entry.email, entry.passwordHash, entry.remember, entry.codeHash, entry.expiresAt, attempts));
            throw invalid();
        }
        if (!accounts.create(entry.email, entry.passwordHash)) {
            pending.remove(id); throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create this account. Try signing in.");
        }
        pending.remove(id);
        return new Verified(entry.email, entry.remember);
    }
}
