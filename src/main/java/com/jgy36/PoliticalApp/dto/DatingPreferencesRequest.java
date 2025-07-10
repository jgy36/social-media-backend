package com.jgy36.PoliticalApp.dto;

import com.jgy36.PoliticalApp.entity.GenderPreference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatingPreferencesRequest {
    private GenderPreference genderPreference;
    private Integer minAge;
    private Integer maxAge;
    private Integer maxDistance;

    // Default constructor
    public DatingPreferencesRequest() {}

    // Constructor with all fields
    public DatingPreferencesRequest(GenderPreference genderPreference, Integer minAge, Integer maxAge, Integer maxDistance) {
        this.genderPreference = genderPreference;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.maxDistance = maxDistance;
    }

    // Getters and Setters
    public GenderPreference getGenderPreference() {
        return genderPreference;
    }

    public void setGenderPreference(GenderPreference genderPreference) {
        this.genderPreference = genderPreference;
    }

    public Integer getMinAge() {
        return minAge;
    }

    public void setMinAge(Integer minAge) {
        this.minAge = minAge;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    public Integer getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(Integer maxDistance) {
        this.maxDistance = maxDistance;
    }

    // Validation method
    public boolean isValid() {
        return genderPreference != null &&
                minAge != null && minAge >= 18 &&
                maxAge != null && maxAge >= minAge && maxAge <= 120 &&
                maxDistance != null && maxDistance > 0;
    }

    @Override
    public String toString() {
        return "DatingPreferencesRequest{" +
                "genderPreference=" + genderPreference +
                ", minAge=" + minAge +
                ", maxAge=" + maxAge +
                ", maxDistance=" + maxDistance +
                '}';
    }
}
