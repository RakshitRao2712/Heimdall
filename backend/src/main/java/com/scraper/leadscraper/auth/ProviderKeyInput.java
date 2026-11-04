package com.scraper.leadscraper.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Accept credentials as input only, and redact accidental diagnostic output. */
public record ProviderKeyInput(@JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String apiKey) {
    @Override
    public String toString() {
        return "ProviderKeyInput[apiKey=REDACTED]";
    }
}
