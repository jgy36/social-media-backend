package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.PhotoMessage;
import com.jgy36.PoliticalApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PhotoMessageRepository extends JpaRepository<PhotoMessage, Long> {

    /**
     * Find photo messages between two users that haven't expired
     */
    @Query("SELECT pm FROM PhotoMessage pm WHERE " +
            "((pm.sender.id = :user1Id AND pm.recipient.id = :user2Id) OR " +
            "(pm.sender.id = :user2Id AND pm.recipient.id = :user1Id)) " +
            "AND pm.expiresAt > :now ORDER BY pm.sentAt DESC")
    List<PhotoMessage> findActivePhotoMessagesBetweenUsers(
            @Param("user1Id") Long user1Id,
            @Param("user2Id") Long user2Id,
            @Param("now") LocalDateTime now);

    /**
     * Find unread photo messages for a user that haven't expired
     */
    @Query("SELECT pm FROM PhotoMessage pm WHERE " +
            "pm.recipient.id = :userId " +
            "AND pm.isViewed = false " +
            "AND pm.expiresAt > :now " +
            "ORDER BY pm.sentAt DESC")
    List<PhotoMessage> findUnreadPhotoMessagesForUser(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    /**
     * Find all photo messages for a recipient (read and unread)
     */
    @Query("SELECT pm FROM PhotoMessage pm WHERE " +
            "pm.recipient.id = :userId " +
            "AND pm.expiresAt > :now " +
            "ORDER BY pm.sentAt DESC")
    List<PhotoMessage> findActivePhotoMessagesForUser(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    /**
     * Find expired photo messages for cleanup
     */
    @Query("SELECT pm FROM PhotoMessage pm WHERE pm.expiresAt <= :now")
    List<PhotoMessage> findExpiredMessages(@Param("now") LocalDateTime now);

    /**
     * Find photo messages that have been viewed maximum times
     */
    @Query("SELECT pm FROM PhotoMessage pm WHERE pm.viewCount >= pm.maxViews")
    List<PhotoMessage> findFullyViewedMessages();

    /**
     * Count unread photo messages for a user
     */
    @Query("SELECT COUNT(pm) FROM PhotoMessage pm WHERE " +
            "pm.recipient.id = :userId " +
            "AND pm.isViewed = false " +
            "AND pm.expiresAt > :now")
    Long countUnreadPhotoMessagesForUser(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    /**
     * Find photo messages where screenshots were taken
     */
    @Query("SELECT pm FROM PhotoMessage pm WHERE " +
            "pm.sender.id = :userId " +
            "AND pm.screenshotTaken = true " +
            "ORDER BY pm.sentAt DESC")
    List<PhotoMessage> findPhotoMessagesWithScreenshots(@Param("userId") Long userId);

    /**
     * Find recent photo messages sent by a user
     */
    @Query("SELECT pm FROM PhotoMessage pm WHERE " +
            "pm.sender.id = :userId " +
            "AND pm.sentAt >= :since " +
            "ORDER BY pm.sentAt DESC")
    List<PhotoMessage> findRecentPhotoMessagesBySender(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    /**
     * Find photo messages in a conversation (both directions)
     */
    List<PhotoMessage> findBySenderAndRecipientOrderBySentAtDesc(User sender, User recipient);

    /**
     * Get photo message conversations with user details and unread counts
     */
    @Query("SELECT DISTINCT " +
            "CASE WHEN pm.sender.id = :userId THEN pm.recipient.id ELSE pm.sender.id END as otherUserId, " +
            "CASE WHEN pm.sender.id = :userId THEN pm.recipient.username ELSE pm.sender.username END as username, " +
            "CASE WHEN pm.sender.id = :userId THEN pm.recipient.displayName ELSE pm.sender.displayName END as displayName, " +
            "CASE WHEN pm.sender.id = :userId THEN pm.recipient.profileImageUrl ELSE pm.sender.profileImageUrl END as profileImageUrl, " +
            "COALESCE(unread.unreadCount, 0) as unreadCount, " +
            "MAX(pm.sentAt) as lastMessageAt " +
            "FROM PhotoMessage pm " +
            "LEFT JOIN (SELECT pm2.recipient.id as recipientId, " +
            "CASE WHEN pm2.sender.id = :userId THEN pm2.recipient.id ELSE pm2.sender.id END as otherUserId2, " +
            "COUNT(*) as unreadCount " +
            "FROM PhotoMessage pm2 " +
            "WHERE pm2.recipient.id = :userId AND pm2.isViewed = false AND pm2.expiresAt > :now " +
            "GROUP BY pm2.recipient.id, otherUserId2) unread " +
            "ON unread.otherUserId2 = CASE WHEN pm.sender.id = :userId THEN pm.recipient.id ELSE pm.sender.id END " +
            "WHERE (pm.sender.id = :userId OR pm.recipient.id = :userId) " +
            "AND pm.expiresAt > :now " +
            "GROUP BY otherUserId, username, displayName, profileImageUrl, unread.unreadCount " +
            "ORDER BY lastMessageAt DESC")
    List<Object[]> findPhotoMessageConversations(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);
}
