package com.jgy36.PoliticalApp.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    /**
     * Get the username of the currently authenticated user
     *
     * @return The username of the authenticated user, or null if not authenticated
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        // Get the principal name (likely an email)
        String principalName = authentication.getName();
        System.out.println("🔑 Principal name: " + principalName);

        // Return the principal name (even if it's an email)
        return principalName;
    }


}
