package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.dto.UserPrivacySettingsDto;
import com.jgy36.PoliticalApp.entity.UserPrivacySettings;
import com.jgy36.PoliticalApp.service.PrivacySettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/privacy-settings")
public class PrivacySettingsController {
    private final PrivacySettingsService privacyService;

    public PrivacySettingsController(PrivacySettingsService privacyService) {
        this.privacyService = privacyService;
    }

    /**
     * ADDED: Initialize privacy settings endpoint (called by frontend)
     */
    @PostMapping("/initialize")
    public ResponseEntity<?> initializePrivacySettings() {
        try {
            UserPrivacySettings settings = privacyService.getCurrentUserSettings();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Privacy settings initialized successfully",
                    "settings", privacyService.toDto(settings)
            ));
        } catch (Exception e) {
            System.err.println("Error initializing privacy settings: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to initialize privacy settings: " + e.getMessage()
            ));
        }
    }

    /**
     * Get privacy settings for the current user
     */
    @GetMapping
    public ResponseEntity<?> getSettings() {
        try {
            UserPrivacySettings settings = privacyService.getCurrentUserSettings();
            return ResponseEntity.ok(privacyService.toDto(settings));
        } catch (Exception e) {
            System.err.println("Error getting privacy settings: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to get privacy settings: " + e.getMessage()
            ));
        }
    }

    /**
     * Update privacy settings for the current user
     */
    @PutMapping
    public ResponseEntity<?> updateSettings(@RequestBody UserPrivacySettingsDto settingsDto) {
        try {
            UserPrivacySettings settings = privacyService.updateCurrentUserSettings(settingsDto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Privacy settings updated successfully",
                    "settings", privacyService.toDto(settings)
            ));
        } catch (Exception e) {
            System.err.println("Error updating privacy settings: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to update privacy settings: " + e.getMessage()
            ));
        }
    }

    /**
     * Reset privacy settings to default for the current user
     */
    @PostMapping("/reset")
    public ResponseEntity<?> resetSettings() {
        try {
            UserPrivacySettings settings = privacyService.resetCurrentUserSettings();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Privacy settings reset to default",
                    "settings", privacyService.toDto(settings)
            ));
        } catch (Exception e) {
            System.err.println("Error resetting privacy settings: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to reset privacy settings: " + e.getMessage()
            ));
        }
    }

    /**
     * Check if a user's account is private (used by frontend to determine follow button behavior)
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<?> checkPrivacyStatus(@PathVariable Long userId) {
        try {
            boolean isPrivate = privacyService.isAccountPrivate(userId);
            return ResponseEntity.ok(Map.of(
                    "isPrivate", isPrivate
            ));
        } catch (Exception e) {
            System.err.println("Error checking privacy status for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to check privacy status: " + e.getMessage()
            ));
        }
    }
}
