// src/main/java/com/jgy36/PoliticalApp/entity/GenderPreference.java
package com.jgy36.PoliticalApp.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GenderPreference {
    MEN("Men"),
    WOMEN("Women"),
    EVERYONE("Everyone"),
    NON_BINARY("Non-binary");

    private final String displayName;

    GenderPreference(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static GenderPreference fromValue(@JsonProperty("value") String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalizedValue = value.trim();

        // Direct display name matches
        for (GenderPreference preference : GenderPreference.values()) {
            if (preference.displayName.equalsIgnoreCase(normalizedValue)) {
                return preference;
            }
        }

        // Enum name matches
        for (GenderPreference preference : GenderPreference.values()) {
            if (preference.name().equalsIgnoreCase(normalizedValue)) {
                return preference;
            }
        }

        // Handle variations
        switch (normalizedValue.toUpperCase()) {
            case "EVERYONE":
            case "EVERY_ONE":
            case "ALL":
                return EVERYONE;
            case "MEN":
            case "MALE":
            case "MALES":
                return MEN;
            case "WOMEN":
            case "FEMALE":
            case "FEMALES":
                return WOMEN;
            case "NON_BINARY":
            case "NON-BINARY":
            case "NONBINARY":
            case "NB":
                return NON_BINARY;
            default:
                throw new IllegalArgumentException("Unknown GenderPreference: " + value);
        }
    }

    // Add static factory method for manual conversion
    public static GenderPreference fromString(String value) {
        return fromValue(value);
    }
}
