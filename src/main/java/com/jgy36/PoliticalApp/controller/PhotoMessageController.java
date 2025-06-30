package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.entity.PhotoMessage;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.service.PhotoMessageService;
import com.jgy36.PoliticalApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/photo-messages")
@CrossOrigin(origins = "http://localhost:3000")
public class PhotoMessageController {

    @Autowired
    private PhotoMessageService photoMessageService;

    @Autowired
    private UserService userService;

    /**
     * Send a photo message
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendPhotoMessage(
            @RequestParam("recipientId") Long recipientId,
            @RequestParam("photo") MultipartFile photo,
            @RequestParam(value = "durationHours", defaultValue = "24") Integer durationHours,
            Authentication authentication) {

        try {
            // Changed from findByUsername to findByEmail since JWT contains email
            User sender = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            User recipient = userService.findById(recipientId)
                    .orElseThrow(() -> new RuntimeException("Recipient not found"));

            PhotoMessage photoMessage = photoMessageService.sendPhotoMessage(
                    sender, recipient, photo, durationHours);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("photoMessageId", photoMessage.getId());
            response.put("expiresAt", photoMessage.getExpiresAt());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * View a photo message (first view or replay)
     */
    @PostMapping("/{photoMessageId}/view")
    public ResponseEntity<?> viewPhotoMessage(
            @PathVariable Long photoMessageId,
            Authentication authentication) {

        try {
            // Changed from findByUsername to findByEmail since JWT contains email
            User viewer = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            PhotoMessage photoMessage = photoMessageService.viewPhotoMessage(photoMessageId, viewer);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("photoUrl", photoMessage.getPhotoUrl());
            response.put("viewCount", photoMessage.getViewCount());
            response.put("maxViews", photoMessage.getMaxViews());
            response.put("hasReplaysLeft", photoMessage.hasReplaysLeft());
            response.put("sender", Map.of(
                    "id", photoMessage.getSender().getId(),
                    "username", photoMessage.getSender().getUsername()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Report screenshot taken
     */
    @PostMapping("/{photoMessageId}/screenshot")
    public ResponseEntity<?> reportScreenshot(
            @PathVariable Long photoMessageId,
            Authentication authentication) {

        try {
            // Changed from findByUsername to findByEmail since JWT contains email
            User screenshotter = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            photoMessageService.reportScreenshot(photoMessageId, screenshotter);

            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get unread photo messages
     */
    @GetMapping("/unread")
    public ResponseEntity<List<PhotoMessage>> getUnreadPhotoMessages(Authentication authentication) {
        // Changed from findByUsername to findByEmail since JWT contains email
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<PhotoMessage> unreadMessages = photoMessageService.getUnreadPhotoMessages(user);
        return ResponseEntity.ok(unreadMessages);
    }

    /**
     * Get photo message conversation between two users
     */
    @GetMapping("/conversation/{otherUserId}")
    public ResponseEntity<List<PhotoMessage>> getPhotoMessageConversation(
            @PathVariable Long otherUserId,
            Authentication authentication) {

        // Changed from findByUsername to findByEmail since JWT contains email
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        User otherUser = userService.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("Other user not found"));

        List<PhotoMessage> messages = photoMessageService.getPhotoMessagesBetweenUsers(user, otherUser);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get photo message metadata (for displaying red squares in UI)
     */
    @GetMapping("/{photoMessageId}/metadata")
    public ResponseEntity<?> getPhotoMessageMetadata(
            @PathVariable Long photoMessageId,
            Authentication authentication) {

        try {
            // Changed from findByUsername to findByEmail since JWT contains email
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // You can add a service method to get metadata without viewing
            // For now, this is a placeholder
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("id", photoMessageId);
            metadata.put("isViewed", false); // Get from database
            metadata.put("hasExpired", false); // Check expiration

            return ResponseEntity.ok(metadata);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
