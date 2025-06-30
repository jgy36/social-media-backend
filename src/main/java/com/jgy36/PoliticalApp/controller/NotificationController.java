package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.entity.Notification;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.entity.UserNotificationPreferences;
import com.jgy36.PoliticalApp.repository.NotificationRepository;
import com.jgy36.PoliticalApp.repository.UserNotificationPreferencesRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import com.jgy36.PoliticalApp.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final UserNotificationPreferencesRepository preferencesRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository, NotificationRepository notificationRepository, UserNotificationPreferencesRepository preferencesRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.preferencesRepository = preferencesRepository;
    }

    // ✅ Get Logged-in User's Notifications
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Notification>> getUserNotifications() {
        return ResponseEntity.ok(notificationService.getUserNotifications());
    }

    // ✅ Mark Notification as Read
    @PutMapping("/{notificationId}/read")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok("Notification marked as read.");
    }

    // ✅ Mark All Notifications as Read
    @PutMapping("/read-all")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok("All notifications marked as read.");
    }

    // Add to NotificationController.java after the existing methods
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Integer>> getUnreadCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        int count = notificationRepository.findByRecipientAndReadFalse(user).size();
        return ResponseEntity.ok(Map.of("count", count));
    }

    // Add to NotificationController.java
    @GetMapping("/debug")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> debugNotifications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get user notification preferences
        UserNotificationPreferences prefs = preferencesRepository.findByUserId(user.getId())
                .orElse(null);

        boolean mentionsEnabled = prefs != null ? prefs.isMentionNotifications() : true;

        // Get recent notifications for this user
        List<Notification> recentNotifications = notificationRepository
                .findByRecipientOrderByCreatedAtDesc(user)
                .stream()
                .limit(5)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "email", user.getEmail(),
                "userId", user.getId(),
                "mentionsEnabled", mentionsEnabled,
                "totalNotifications", notificationRepository.findByRecipientOrderByCreatedAtDesc(user).size(),
                "unreadCount", notificationRepository.findByRecipientAndReadFalse(user).size(),
                "recentNotifications", recentNotifications
        ));
    }
}
