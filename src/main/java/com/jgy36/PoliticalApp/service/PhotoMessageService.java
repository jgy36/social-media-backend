package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.entity.Match;
import com.jgy36.PoliticalApp.entity.PhotoMessage;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.repository.FollowRepository;
import com.jgy36.PoliticalApp.repository.MatchRepository;
import com.jgy36.PoliticalApp.repository.PhotoMessageRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class PhotoMessageService {

    @Autowired
    private PhotoMessageRepository photoMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private FollowRepository followRepository;

    private final String UPLOAD_DIR = "uploads/photo-messages/";

    /**
     * Send a photo message - checks if users can send to each other
     */
    public PhotoMessage sendPhotoMessage(User sender, User recipient, MultipartFile photo, Integer durationHours) {
        // Check if sender can send to recipient (must be following or matched)
        if (!canSendPhotoMessage(sender, recipient)) {
            throw new RuntimeException("Cannot send photo message to this user. Must be following or matched.");
        }

        try {
            // Save photo file
            String photoUrl = savePhotoFile(photo);

            // Create photo message
            PhotoMessage photoMessage = new PhotoMessage();
            photoMessage.setSender(sender);
            photoMessage.setRecipient(recipient);
            photoMessage.setPhotoUrl(photoUrl);
            photoMessage.setSentAt(LocalDateTime.now());
            photoMessage.setExpiresAt(LocalDateTime.now().plusHours(durationHours != null ? durationHours : 24));
            photoMessage.setIsViewed(false);
            photoMessage.setViewCount(0);
            photoMessage.setMaxViews(2); // View once + replay once

            return photoMessageRepository.save(photoMessage);

        } catch (IOException e) {
            throw new RuntimeException("Failed to save photo message", e);
        }
    }

    /**
     * Check if user can send photo message to another user
     */
    private boolean canSendPhotoMessage(User sender, User recipient) {
        // Can send if:
        // 1. They are matched (dating)
        // 2. Sender follows recipient (social media)

        boolean isMatched = matchRepository.findActiveMatchBetweenUsers(sender, recipient).isPresent();
        boolean isFollowing = followRepository.existsByFollowerAndFollowing(sender, recipient);

        return isMatched || isFollowing;
    }

    /**
     * View a photo message (first view or replay)
     */
    public PhotoMessage viewPhotoMessage(Long photoMessageId, User viewer) {
        PhotoMessage photoMessage = photoMessageRepository.findById(photoMessageId)
                .orElseThrow(() -> new RuntimeException("Photo message not found"));

        // Check if viewer is the recipient
        if (!photoMessage.getRecipient().getId().equals(viewer.getId())) {
            throw new RuntimeException("Not authorized to view this photo message");
        }

        // Check if expired
        if (LocalDateTime.now().isAfter(photoMessage.getExpiresAt())) {
            throw new RuntimeException("Photo message has expired");
        }

        // Check if already viewed max times
        if (photoMessage.getViewCount() >= photoMessage.getMaxViews()) {
            throw new RuntimeException("Photo message has been viewed maximum times");
        }

        // Update view info
        photoMessage.setViewCount(photoMessage.getViewCount() + 1);

        if (!photoMessage.getIsViewed()) {
            photoMessage.setIsViewed(true);
            photoMessage.setFirstViewedAt(LocalDateTime.now());
        } else {
            photoMessage.setLastViewedAt(LocalDateTime.now());
        }

        return photoMessageRepository.save(photoMessage);
    }

    /**
     * Report screenshot taken
     */
    public void reportScreenshot(Long photoMessageId, User screenshotter) {
        PhotoMessage photoMessage = photoMessageRepository.findById(photoMessageId)
                .orElseThrow(() -> new RuntimeException("Photo message not found"));

        // Create screenshot notification for sender
        // You can integrate this with your existing notification system
        createScreenshotNotification(photoMessage.getSender(), screenshotter, photoMessage);
    }

    /**
     * Get unread photo messages for user
     */
    public List<PhotoMessage> getUnreadPhotoMessages(User user) {
        LocalDateTime now = LocalDateTime.now();
        return photoMessageRepository.findUnreadPhotoMessagesForUser(user.getId(), now);
    }

    /**
     * Get conversation photo messages between two users
     */
    public List<PhotoMessage> getPhotoMessagesBetweenUsers(User user1, User user2) {
        LocalDateTime now = LocalDateTime.now();
        return photoMessageRepository.findActivePhotoMessagesBetweenUsers(user1.getId(), user2.getId(), now);
    }

    /**
     * Clean up expired photo messages
     */
    public void cleanupExpiredMessages() {
        LocalDateTime now = LocalDateTime.now();
        List<PhotoMessage> expiredMessages = photoMessageRepository.findExpiredMessages(now);

        for (PhotoMessage message : expiredMessages) {
            // Delete photo file
            deletePhotoFile(message.getPhotoUrl());
            // Delete database record
            photoMessageRepository.delete(message);
        }
    }

    private String savePhotoFile(MultipartFile file) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = UUID.randomUUID().toString() + extension;

        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        return UPLOAD_DIR + filename;
    }

    private void deletePhotoFile(String photoUrl) {
        try {
            Path filePath = Paths.get(photoUrl);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log error but don't throw - cleanup should continue
            System.err.println("Failed to delete photo file: " + photoUrl);
        }
    }

    private void createScreenshotNotification(User sender, User screenshotter, PhotoMessage photoMessage) {
        // TODO: Integrate with your existing notification system
        // Create notification: "{screenshotter.username} took a screenshot of your photo"
        System.out.println("Screenshot taken by " + screenshotter.getUsername() +
                " of photo message from " + sender.getUsername());
    }
    // ADD this method to your PhotoMessageService.java class

    /**
     * Get all photo message conversations including new dating matches
     */
    public List<Map<String, Object>> getPhotoMessageConversationsWithMatches(User user) {
        List<Map<String, Object>> conversations = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Get existing photo message conversations
        List<Object[]> existingConversations = photoMessageRepository.findPhotoMessageConversations(user.getId(), now);

        for (Object[] row : existingConversations) {
            Long otherUserId = (Long) row[0];
            String username = (String) row[1];
            String displayName = (String) row[2];
            String profileImageUrl = (String) row[3];
            Long unreadCount = (Long) row[4];
            LocalDateTime lastMessageAt = (LocalDateTime) row[5];

            Map<String, Object> conversation = new HashMap<>();
            conversation.put("userId", otherUserId);
            conversation.put("username", username);
            conversation.put("displayName", displayName);
            conversation.put("profileImageUrl", profileImageUrl);
            conversation.put("unreadCount", unreadCount);
            conversation.put("lastMessageAt", lastMessageAt);
            conversation.put("isMatch", false);
            conversation.put("isNewMatch", false);

            conversations.add(conversation);
        }

        // Get dating matches that don't have photo conversations yet
        List<Match> userMatches = matchRepository.findActiveMatchesForUser(user);

        for (Match match : userMatches) {
            User otherUser = match.getUser1().getId().equals(user.getId()) ?
                    match.getUser2() : match.getUser1();

            // Check if they already have a photo conversation
            boolean hasExistingConversation = conversations.stream()
                    .anyMatch(conv -> conv.get("userId").equals(otherUser.getId()));

            if (!hasExistingConversation) {
                Map<String, Object> matchConversation = new HashMap<>();
                matchConversation.put("userId", otherUser.getId());
                matchConversation.put("username", otherUser.getUsername());
                matchConversation.put("displayName", otherUser.getDisplayName());
                matchConversation.put("profileImageUrl", otherUser.getProfileImageUrl());
                matchConversation.put("unreadCount", 0L);
                matchConversation.put("lastMessageAt", match.getMatchedAt());
                matchConversation.put("isMatch", true);
                matchConversation.put("isNewMatch", isNewMatch(match)); // Check if match is recent
                matchConversation.put("matchedAt", match.getMatchedAt());

                conversations.add(matchConversation);
            } else {
                // Update existing conversation to mark as match
                conversations.stream()
                        .filter(conv -> conv.get("userId").equals(otherUser.getId()))
                        .findFirst()
                        .ifPresent(conv -> {
                            conv.put("isMatch", true);
                            conv.put("matchedAt", match.getMatchedAt());
                        });
            }
        }

        // Sort by most recent activity (lastMessageAt or matchedAt)
        conversations.sort((a, b) -> {
            LocalDateTime dateA = (LocalDateTime) a.get("lastMessageAt");
            LocalDateTime dateB = (LocalDateTime) b.get("lastMessageAt");
            return dateB.compareTo(dateA);
        });

        return conversations;
    }

    /**
     * Check if a match is "new" (within last 24 hours and no photo messages exchanged)
     */
    private boolean isNewMatch(Match match) {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        // Match is new if it happened in last 24 hours
        boolean isRecent = match.getMatchedAt().isAfter(oneDayAgo);

        if (!isRecent) return false;

        // Check if they've exchanged any photo messages since matching
        User user1 = match.getUser1();
        User user2 = match.getUser2();

        List<PhotoMessage> messagesBetween = photoMessageRepository
                .findActivePhotoMessagesBetweenUsers(user1.getId(), user2.getId(), LocalDateTime.now());

        // If they have messages since matching, it's not "new" anymore
        boolean hasMessagesAfterMatch = messagesBetween.stream()
                .anyMatch(msg -> msg.getSentAt().isAfter(match.getMatchedAt()));

        return !hasMessagesAfterMatch;
    }
}
