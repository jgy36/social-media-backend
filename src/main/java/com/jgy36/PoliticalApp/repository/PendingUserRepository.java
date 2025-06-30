package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.PendingUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PendingUserRepository extends JpaRepository<PendingUser, Long> {
    Optional<PendingUser> findByVerificationToken(String token);

    Optional<PendingUser> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Clean up expired pending registrations
    @Modifying
    @Query("DELETE FROM PendingUser p WHERE p.expiresAt < :now")
    void deleteExpiredPendingUsers(@Param("now") LocalDateTime now);
}
