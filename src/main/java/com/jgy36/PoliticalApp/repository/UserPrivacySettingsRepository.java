package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.entity.UserPrivacySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPrivacySettingsRepository extends JpaRepository<UserPrivacySettings, Long> {

    /**
     * Find privacy settings by user
     */
    Optional<UserPrivacySettings> findByUser(User user);

    /**
     * Find privacy settings by user ID
     */
    @Query("SELECT ups FROM UserPrivacySettings ups WHERE ups.user.id = :userId")
    Optional<UserPrivacySettings> findByUserId(@Param("userId") Long userId);

    /**
     * Check if user has privacy settings configured
     */
    boolean existsByUser(User user);

    /**
     * Check if user has privacy settings configured by user ID
     */
    @Query("SELECT COUNT(ups) > 0 FROM UserPrivacySettings ups WHERE ups.user.id = :userId")
    boolean existsByUserId(@Param("userId") Long userId);

    /**
     * Delete privacy settings by user
     */
    void deleteByUser(User user);

    /**
     * Find all users with public profiles
     */
    @Query("SELECT ups FROM UserPrivacySettings ups WHERE ups.profilePublic = true")
    List<UserPrivacySettings> findUsersWithPublicProfiles();

    /**
     * Find users who allow showing posts to matches
     */
    @Query("SELECT ups FROM UserPrivacySettings ups WHERE ups.showPostsToMatches = true")
    List<UserPrivacySettings> findUsersWhoShowPostsToMatches();
}
