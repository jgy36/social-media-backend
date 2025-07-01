package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.Role;
import com.jgy36.PoliticalApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    List<User> findByRole(Role role);

    Optional<User> findByVerificationToken(String token);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    // ✅ Fetch follower count
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.following.id = :userId")
    int countFollowers(Long userId);

    // ✅ Fetch following count
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.follower.id = :userId")
    int countFollowing(Long userId);

    // ✅ Fetch post count
    @Query("SELECT COUNT(p) FROM Post p WHERE p.author.id = :userId")
    int countPosts(Long userId);

    // Basic username search
    List<User> findByUsernameContainingIgnoreCase(String query);

    // ✅ Search by username OR displayName (for search functionality)
    List<User> findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(String username, String displayName);

    // ✅ Add this new method for case-insensitive username check
    boolean existsByUsernameIgnoreCase(String username);

    // New methods for settings functionality

    /**
     * Delete user and all associated data
     */
    @Modifying
    @Query("DELETE FROM User u WHERE u.id = :userId")
    void deleteUserById(Long userId);

    /**
     * Find users with expired verification tokens
     */
    @Query("SELECT u FROM User u WHERE u.verificationTokenExpiresAt < CURRENT_TIMESTAMP AND u.verified = false")
    List<User> findUsersWithExpiredVerificationTokens();

    /**
     * Find top matching users ordered by username (simple alternative)
     * We removed the followers-based sorting which was causing errors
     */
    @Query("SELECT u FROM User u WHERE u.username LIKE %:query% OR u.displayName LIKE %:query% ORDER BY u.username")
    List<User> findTopMatchingUsers(String query, org.springframework.data.domain.Pageable pageable);

    List<User> findByEmailVerifiedFalse();


    long countByEmailContaining(String emailPattern);

    // Add this method to your existing UserRepository interface:
    List<User> findByEmailContaining(String emailPattern);
    long count(); // If this doesn't exist already

}
