package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.Conversation;
import com.jgy36.PoliticalApp.entity.Message;
import com.jgy36.PoliticalApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Find messages by conversation, ordered by sent time
    List<Message> findByConversationOrderBySentAtAsc(Conversation conversation);

    // Find unread messages for a user in a conversation
    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation AND m.sender != :user AND m.read = false")
    List<Message> findUnreadMessagesInConversation(@Param("conversation") Conversation conversation, @Param("user") User user);

    // Count unread messages for a user
    @Query("SELECT COUNT(m) FROM Message m JOIN m.conversation c JOIN c.participants p " +
            "WHERE p = :user AND m.sender != :user AND m.read = false")
    int countUnreadMessagesForUser(@Param("user") User user);

    // Find recent messages for a user, one per conversation, ordered by sent time
    @Query("SELECT m FROM Message m WHERE m.id IN " +
            "(SELECT MAX(m2.id) FROM Message m2 JOIN m2.conversation c JOIN c.participants p " +
            "WHERE p = :user GROUP BY c.id) " +
            "ORDER BY m.sentAt DESC")
    List<Message> findRecentMessagesForUser(@Param("user") User user);
}
