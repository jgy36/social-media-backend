package com.jgy36.PoliticalApp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PoliticianApiResponse {

    private String kind;
    private NormalizedInput normalizedInput;
    private Map<String, Division> divisions;
    private List<Office> offices;
    private List<Official> officials;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NormalizedInput {
        private String line1;
        private String city;
        private String state;
        private String zip;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Division {
        private String name;
        private List<Integer> officeIndices;
        private String alsoKnownAs;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Office {
        private String name;
        private String divisionId;
        private List<String> levels;
        private List<String> roles;
        private List<Integer> officialIndices;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Official {
        private String name;
        private String party;
        private List<Address> address;
        private List<String> phones;
        private List<String> emails;
        private List<String> urls;
        private String photoUrl;
        private List<Channel> channels;

        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Address {
            private String line1;
            private String line2;
            private String line3;
            private String city;
            private String state;
            private String zip;
        }

        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Channel {
            private String type;
            private String id;
        }
    }
}
