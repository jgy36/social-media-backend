package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.entity.*;
import com.jgy36.PoliticalApp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
}
