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
     * Get privacy settings for the current user
     */
    @GetMapping
    public ResponseEntity<?> getSettings() {
        UserPrivacySettings settings = privacyService.getCurrentUserSettings();
        return ResponseEntity.ok(privacyService.toDto(settings));
    }

    /**
     * Update privacy settings for the current user
     */
    @PutMapping
    public ResponseEntity<?> updateSettings(@RequestBody UserPrivacySettingsDto settingsDto) {
        UserPrivacySettings settings = privacyService.updateCurrentUserSettings(settingsDto);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Privacy settings updated successfully",
                "settings", privacyService.toDto(settings)
        ));
    }

    /**
     * Reset privacy settings to default for the current user
     */
    @PostMapping("/reset")
    public ResponseEntity<?> resetSettings() {
        UserPrivacySettings settings = privacyService.resetCurrentUserSettings();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Privacy settings reset to default",
                "settings", privacyService.toDto(settings)
        ));
    }

    /**
     * Check if a user's account is private (used by frontend to determine follow button behavior)
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<?> checkPrivacyStatus(@PathVariable Long userId) {
        boolean isPrivate = privacyService.isAccountPrivate(userId);

        return ResponseEntity.ok(Map.of(
                "isPrivate", isPrivate
        ));
    }
}
