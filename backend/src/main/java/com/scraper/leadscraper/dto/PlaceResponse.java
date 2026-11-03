package com.scraper.leadscraper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PlaceResponse(List<Place> places) {
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record Place(
            @JsonProperty("displayName") DisplayName displayName,
            @JsonProperty("websiteUri") String websiteUri,
            @JsonProperty("formattedAddress") String formattedAddress,
            @JsonProperty("nationalPhoneNumber") String nationalPhoneNumber,
            @JsonProperty("businessStatus") String businessStatus,
            @JsonProperty("userRatingCount") Integer userRatingCount,
            @JsonProperty("id") String id) {
        public Place(DisplayName displayName,String websiteUri,String formattedAddress,String nationalPhoneNumber,String businessStatus,Integer userRatingCount){this(displayName,websiteUri,formattedAddress,nationalPhoneNumber,businessStatus,userRatingCount,null);}
        public record DisplayName(String text) {
        }
    }
}
