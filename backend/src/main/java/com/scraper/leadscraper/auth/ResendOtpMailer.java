package com.scraper.leadscraper.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Service
public class ResendOtpMailer implements OtpMailer {
    private final String apiKey, from;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    public ResendOtpMailer(@Value("${RESEND_API_KEY:}") String apiKey, @Value("${HEIMDALL_MAIL_FROM:}") String from, ObjectMapper mapper) {
        this.apiKey = apiKey; this.from = from; this.mapper = mapper;
    }
    public void send(String email, String code) {
        if (apiKey.isBlank() || from.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email verification is not configured yet. Please contact the Heimdall operator.");
        try {
            String body = mapper.writeValueAsString(Map.of(
                "from", from, "to", List.of(email), "subject", "Your Heimdall verification code",
                "text", "Your Heimdall verification code is " + code + ". It expires in 10 minutes. If you did not request this, ignore this email. Do not share this code.",
                "html", "<div style=\"font-family:Arial,sans-serif;padding:32px;color:#171717\"><h2>Welcome to Heimdall</h2><p>Enter this code to verify your email:</p><p style=\"font-size:32px;letter-spacing:8px;color:#e80082;font-weight:700\">" + code + "</p><p>This code expires in 10 minutes. If you did not request this, ignore this email.</p><p>Do not share this code.</p></div>"));
            var request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var response = http.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not send the verification email. Please try again shortly.");
        } catch (ResponseStatusException e) { throw e; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email delivery was interrupted. Please retry."); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not send the verification email. Please try again shortly."); }
    }
}
