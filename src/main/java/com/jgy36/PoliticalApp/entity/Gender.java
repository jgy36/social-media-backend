// src/main/java/com/jgy36/PoliticalApp/entity/Gender.java
package com.jgy36.PoliticalApp.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Gender {
    MAN("Man"),
    WOMAN("Woman"),
    NON_BINARY("Non-binary"),
    OTHER("Other");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static Gender fromValue(@JsonProperty("value") String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalizedValue = value.trim();

        // Direct display name matches
        for (Gender gender : Gender.values()) {
            if (gender.displayName.equalsIgnoreCase(normalizedValue)) {
                return gender;
            }
        }

        // Enum name matches
        for (Gender gender : Gender.values()) {
            if (gender.name().equalsIgnoreCase(normalizedValue)) {
                return gender;
            }
        }

        // Handle variations
        switch (normalizedValue.toUpperCase()) {
            case "MAN":
            case "MALE":
                return MAN;
            case "WOMAN":
            case "FEMALE":
                return WOMAN;
            case "NON_BINARY":
            case "NON-BINARY":
            case "NONBINARY":
            case "NB":
                return NON_BINARY;
            case "OTHER":
            case "OTHERS":
                return OTHER;
            default:
                throw new IllegalArgumentException("Unknown Gender: " + value);
        }
    }

    // Add static factory method for manual conversion
    public static Gender fromString(String value) {
        return fromValue(value);
    }
}
