package com.scraper.leadscraper.service;
import java.util.List;
public record SearchProgress(int target, int checked, int requests, int found, String status, String reason) {
 public record Outcome<T>(List<T> leads, SearchProgress progress) { }
}
