package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.dto.UserNotificationPreferencesDto;
import com.jgy36.PoliticalApp.entity.UserNotificationPreferences;
import com.jgy36.PoliticalApp.service.NotificationPreferencesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/notification-preferences")
public class NotificationPreferencesController {
    private final NotificationPreferencesService preferencesService;

    public NotificationPreferencesController(NotificationPreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    /**
     * Get notification preferences for the current user
     */
    @GetMapping
    public ResponseEntity<?> getPreferences() {
        UserNotificationPreferences preferences = preferencesService.getCurrentUserPreferences();
        return ResponseEntity.ok(preferencesService.toDto(preferences));
    }

    /**
     * Update notification preferences for the current user
     */
    @PutMapping
    public ResponseEntity<?> updatePreferences(@RequestBody UserNotificationPreferencesDto preferencesDto) {
        UserNotificationPreferences preferences = preferencesService.updateCurrentUserPreferences(preferencesDto);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Notification preferences updated successfully",
                "preferences", preferencesService.toDto(preferences)
        ));
    }

    /**
     * Reset notification preferences to default for the current user
     */
    @PostMapping("/reset")
    public ResponseEntity<?> resetPreferences() {
        UserNotificationPreferences preferences = preferencesService.resetCurrentUserPreferences();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Notification preferences reset to default",
                "preferences", preferencesService.toDto(preferences)
        ));
    }
}
