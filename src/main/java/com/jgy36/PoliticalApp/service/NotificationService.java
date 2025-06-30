package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.entity.*;
import com.jgy36.PoliticalApp.repository.NotificationRepository;
import com.jgy36.PoliticalApp.repository.UserNotificationPreferencesRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserNotificationPreferencesRepository preferencesRepository;


    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository, UserNotificationPreferencesRepository preferencesRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.preferencesRepository = preferencesRepository;
    }

    // ✅ Fetch Notifications for Logged-in User
    public List<Notification> getUserNotifications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
    }

    // ✅ Mark a notification as read
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    // ✅ Utility: Create a new notification
    public void createNotification(User recipient, String message) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }

    public void createNotification(User recipient, String message, String notificationType,
                                   Long referenceId, Long secondaryReferenceId, String communityId) {
        // Check user preferences before creating notification
        if (!shouldSendNotification(recipient, notificationType)) {
            return; // Don't create notification if user has disabled this type
        }

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setMessage(message);
        notification.setNotificationType(notificationType);
        notification.setReferenceId(referenceId);
        notification.setSecondaryReferenceId(secondaryReferenceId);
        notification.setCommunityId(communityId);
        notificationRepository.save(notification);
    }

    // Update the markAllAsRead method in NotificationService.java
    public void markAllAsRead() {
        // Get current user using the same approach as in getUserNotifications
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Find all unread notifications for the user
        List<Notification> unreadNotifications = notificationRepository.findByRecipientAndReadFalse(currentUser);

        // Mark each as read
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
        }

        // Save all updated notifications
        notificationRepository.saveAll(unreadNotifications);
    }


    // Simplified overload for common cases
    public void createPostNotification(User recipient, User actor, Post post) {
        String message = actor.getUsername() + " posted in " + post.getCommunity().getName();
        createNotification(recipient, message, "post_created", post.getId(), null,
                post.getCommunity().getSlug());
    }

    // Check user preferences before creating notification
    private boolean shouldSendNotification(User recipient, String notificationType) {
        UserNotificationPreferences prefs = preferencesRepository.findByUserId(recipient.getId())
                .orElse(null);

        if (prefs == null) {
            System.out.println("DEBUG: No notification preferences found for user " + recipient.getUsername() + ", using defaults");
            return true; // Default to sending if no preferences found
        }

        boolean shouldSend = switch (notificationType) {
            case "comment_created" -> {
                boolean result = prefs.isNewCommentNotifications();
                System.out.println("DEBUG: User " + recipient.getUsername() + " comment notifications enabled: " + result);
                yield result;
            }
            case "mention" -> {
                boolean result = prefs.isMentionNotifications();
                System.out.println("DEBUG: User " + recipient.getUsername() + " mention notifications enabled: " + result);
                yield result;
            }
            case "like" -> {
                boolean result = prefs.isLikeNotifications();
                System.out.println("DEBUG: User " + recipient.getUsername() + " like notifications enabled: " + result);
                yield result;
            }
            case "follow", "follow_request" -> {
                boolean result = prefs.isFollowNotifications();
                System.out.println("DEBUG: User " + recipient.getUsername() + " follow notifications enabled: " + result);
                yield result;
            }
            case "direct_message" -> {
                boolean result = prefs.isDirectMessageNotifications();
                System.out.println("DEBUG: User " + recipient.getUsername() + " direct message notifications enabled: " + result);
                yield result;
            }
            case "community_update" -> {
                boolean result = prefs.isCommunityUpdates();
                System.out.println("DEBUG: User " + recipient.getUsername() + " community update notifications enabled: " + result);
                yield result;
            }
            default -> {
                System.out.println("DEBUG: Unknown notification type: " + notificationType + " for user " + recipient.getUsername() + ", defaulting to true");
                yield true;
            }
        };

        System.out.println("DEBUG: Final notification decision for user " + recipient.getUsername() +
                ", type: " + notificationType + " = " + shouldSend);

        return shouldSend;
    }

    // Create comment notification
    public void createCommentNotification(User recipient, User commenter, Post post, Comment comment) {
        if (!shouldSendNotification(recipient, "comment_created")) return;

        String message = commenter.getUsername() + " commented on your post";
        createNotification(
                recipient,
                message,
                "comment_created",
                post.getId(),  // Post ID as primary reference
                comment.getId(),  // Comment ID as secondary reference
                post.getCommunity() != null ? post.getCommunity().getSlug() : null
        );
    }

    // Create like notification
    public void createLikeNotification(User recipient, User liker, Object likedObject, boolean isComment) {
        if (!shouldSendNotification(recipient, "like")) return;

        String objectType = isComment ? "comment" : "post";
        String message = liker.getUsername() + " liked your " + objectType;

        Long primaryId = null;
        Long secondaryId = null;
        String communityId = null;

        if (isComment) {
            Comment comment = (Comment) likedObject;
            primaryId = comment.getPost().getId();  // Post ID
            secondaryId = comment.getId();  // Comment ID
            communityId = comment.getPost().getCommunity() != null ?
                    comment.getPost().getCommunity().getSlug() : null;
        } else {
            Post post = (Post) likedObject;
            primaryId = post.getId();  // Post ID
            communityId = post.getCommunity() != null ? post.getCommunity().getSlug() : null;
        }

        createNotification(recipient, message, "like", primaryId, secondaryId, communityId);
    }

    // Create mention notification
    public void createMentionNotification(User mentioned, User mentioner, Post post, Comment comment) {
        if (!shouldSendNotification(mentioned, "mention")) return;

        String context = comment != null ? "a comment" : "a post";
        String message = mentioner.getUsername() + " mentioned you in " + context;

        Long primaryId = post.getId();  // Post ID is always primary
        Long secondaryId = comment != null ? comment.getId() : null;  // Comment ID if applicable
        String communityId = post.getCommunity() != null ? post.getCommunity().getSlug() : null;

        createNotification(mentioned, message, "mention", primaryId, secondaryId, communityId);
    }

    // Create follow notification
    public void createFollowNotification(User recipient, User follower) {
        if (!shouldSendNotification(recipient, "follow")) return;

        String message = follower.getUsername() + " started following you";
        createNotification(recipient, message, "follow", follower.getId(), null, null);
    }

    // Create follow request notification
    public void createFollowRequestNotification(User recipient, User requester) {
        if (!shouldSendNotification(recipient, "follow_request")) return;

        String message = requester.getUsername() + " requested to follow you";
        createNotification(recipient, message, "follow_request", requester.getId(), null, null);
    }
}
