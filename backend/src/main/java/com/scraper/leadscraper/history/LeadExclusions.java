package com.scraper.leadscraper.history;

import java.text.Normalizer;
import java.util.*;
import java.util.function.Predicate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Resolve only the signed-in account's files, then match business identity, not just name. */
@Service
public class LeadExclusions {
    private final LeadFiles files;
    public LeadExclusions(LeadFiles files) { this.files = files; }

    public Predicate<SearchHistory.Lead> fromFiles(String account, List<String> ids) {
        if (ids == null || ids.isEmpty()) return lead -> false;
        if (ids.size() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select up to 100 lead files.");
        var selected = new HashSet<>(ids);
        var owned = files.list(account).stream().filter(file -> selected.contains(file.id())).toList();
        if (owned.size() != selected.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A selected lead file is no longer available. Refresh your files and select again.");
        var identities = new HashSet<String>();
        owned.forEach(file -> file.leads().forEach(lead -> identities.addAll(keys(lead))));
        return lead -> keys(lead).stream().anyMatch(identities::contains);
    }

    static Set<String> keys(SearchHistory.Lead lead) {
        var keys = new HashSet<String>();
        if (lead.providerId() != null && !lead.providerId().isBlank()) keys.add("provider:" + lead.providerId());
        String name = normalize(lead.name());
        String address = normalize(lead.address());
        if (name.isEmpty()) return keys;
        if (!address.isEmpty()) keys.add("address:" + name + "|" + address);
        if (lead.phones() != null) for (String phone : lead.phones()) {
            if (phone == null) continue;
            String digits = phone.replaceAll("[^0-9]", "");
            if (digits.length() >= 7) keys.add("phone:" + name + "|" + digits);
        }
        // A name alone could incorrectly exclude another branch of the same business.
        return keys;
    }
    private static String normalize(String text) {
        return text == null ? "" : Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
