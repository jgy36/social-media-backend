package com.jgy36.PoliticalApp.utils;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OAuth2Util {
    private final OAuth2AuthorizedClientService authorizedClientService;

    public OAuth2Util(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    /**
     * Get OAuth2 client for a given authentication token
     */
    public OAuth2AuthorizedClient getAuthorizedClient(OAuth2AuthenticationToken authentication) {
        return authorizedClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName()
        );
    }

    /**
     * Extract provider user ID from OAuth2 user attributes
     */
    public String getProviderUserId(OAuth2User oAuth2User, String providerName) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        switch (providerName.toLowerCase()) {
            case "google":
                return (String) attributes.get("sub");
            case "facebook":
                return (String) attributes.get("id");
            case "twitter":
                return (String) attributes.get("id_str");
            case "github":
                return String.valueOf(attributes.get("id"));
            default:
                throw new IllegalArgumentException("Unsupported provider: " + providerName);
        }
    }

    /**
     * Extract user email from OAuth2 user attributes
     */
    public String getUserEmail(OAuth2User oAuth2User, String providerName) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        switch (providerName.toLowerCase()) {
            case "google":
            case "github":
                return (String) attributes.get("email");
            case "facebook":
                return (String) attributes.get("email");
            case "twitter":
                // Twitter doesn't provide email by default
                return (String) attributes.getOrDefault("email", null);
            default:
                throw new IllegalArgumentException("Unsupported provider: " + providerName);
        }
    }

    /**
     * Extract user name from OAuth2 user attributes
     */
    public String getUserName(OAuth2User oAuth2User, String providerName) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        switch (providerName.toLowerCase()) {
            case "google":
                return (String) attributes.get("name");
            case "facebook":
                return (String) attributes.get("name");
            case "twitter":
                return (String) attributes.get("name");
            case "github":
                return (String) attributes.get("login");
            default:
                throw new IllegalArgumentException("Unsupported provider: " + providerName);
        }
    }
}
