package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.Conversation;
import com.jgy36.PoliticalApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // Find conversations where a user is a participant
    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p = :user ORDER BY c.updatedAt DESC")
    List<Conversation> findConversationsByParticipant(@Param("user") User user);

    // Find a direct conversation between two users
    @Query("SELECT c FROM Conversation c JOIN c.participants p1 JOIN c.participants p2 " +
            "WHERE p1 = :user1 AND p2 = :user2 AND SIZE(c.participants) = 2")
    Optional<Conversation> findDirectConversation(@Param("user1") User user1, @Param("user2") User user2);

    // Search conversations by participant username (for finding users to message)
    @Query("SELECT DISTINCT c FROM Conversation c JOIN c.participants p " +
            "WHERE p.username LIKE %:query% AND c IN " +
            "(SELECT c2 FROM Conversation c2 JOIN c2.participants p2 WHERE p2 = :currentUser)")
    List<Conversation> searchConversationsByUsername(@Param("query") String query, @Param("currentUser") User currentUser);
}
