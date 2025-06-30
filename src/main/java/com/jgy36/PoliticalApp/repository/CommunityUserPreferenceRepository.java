package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.Community;
import com.jgy36.PoliticalApp.entity.CommunityUserPreference;
import com.jgy36.PoliticalApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommunityUserPreferenceRepository extends JpaRepository<CommunityUserPreference, Long> {

    // Find preferences for a specific user and community
    Optional<CommunityUserPreference> findByUserAndCommunity(User user, Community community);

    // Check if a preference exists
    boolean existsByUserAndCommunity(User user, Community community);

    // Find all preferences for a specific user
    Iterable<CommunityUserPreference> findAllByUser(User user);

    // Find all preferences for a specific community
    Iterable<CommunityUserPreference> findAllByCommunity(Community community);
}
