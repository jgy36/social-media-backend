package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.entity.*;
import com.jgy36.PoliticalApp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrivacyService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserPrivacySettingsRepository userPrivacySettingsRepository;

    /**
     * Check if viewer can see user's social media content
     */
    public boolean canViewSocialProfile(User viewer, User profileOwner) {
        // Public profiles can be viewed by anyone
        UserPrivacySettings privacy = getUserPrivacySettings(profileOwner);
        if (privacy.isProfilePublic()) {
            return true;
        }

        // If viewer is following, they can see content
        if (followRepository.existsByFollowerAndFollowing(viewer, profileOwner)) {
            return true;
        }

        // If they are matched, they can see content based on match privacy settings
        if (areMatched(viewer, profileOwner)) {
            return privacy.isShowPostsToMatches();
        }

        return false;
    }

    /**
     * Get posts that a viewer is allowed to see from a user
     */
    public List<Post> getVisiblePostsForViewer(User viewer, User profileOwner) {
        if (!canViewSocialProfile(viewer, profileOwner)) {
            return List.of(); // Empty list if can't view profile
        }

        UserPrivacySettings privacy = getUserPrivacySettings(profileOwner);
        List<Post> allPosts = postRepository.findByAuthorOrderByCreatedAtDesc(profileOwner);


        // If they're matched (not following), apply match-specific privacy
        if (areMatched(viewer, profileOwner) && !followRepository.existsByFollowerAndFollowing(viewer, profileOwner)) {
            return filterPostsForMatches(allPosts, privacy);
        }

        // If following or public, show all posts
        return allPosts;
    }

    /**
     * Filter posts based on what matches are allowed to see
     */
    private List<Post> filterPostsForMatches(List<Post> posts, UserPrivacySettings privacy) {
        return posts.stream()
                .filter(post -> {
                    // Only show posts from last X days to matches
                    if (privacy.getMatchPostsTimeLimit() != null) {
                        return post.getCreatedAt().isAfter(
                                java.time.LocalDateTime.now().minusDays(privacy.getMatchPostsTimeLimit())
                        );
                    }
                    return true;
                })
                .limit(privacy.getMaxPostsForMatches() != null ? privacy.getMaxPostsForMatches() : Long.MAX_VALUE)
                .collect(Collectors.toList());
    }

    /**
     * Check if two users are matched
     */
    public boolean areMatched(User user1, User user2) {
        return matchRepository.findActiveMatchBetweenUsers(user1, user2).isPresent();
    }

    /**
     * Get user's privacy settings (or create default if none exist)
     */
    public UserPrivacySettings getUserPrivacySettings(User user) {
        return userPrivacySettingsRepository.findByUser(user)
                .orElseGet(() -> createDefaultPrivacySettings(user));
    }

    /**
     * Create default privacy settings for a user
     */
    private UserPrivacySettings createDefaultPrivacySettings(User user) {
        UserPrivacySettings settings = new UserPrivacySettings();
        settings.setUser(user);
        settings.setProfilePublic(true);
        settings.setShowPostsToMatches(true);
        settings.setMaxPostsForMatches(10); // Show last 10 posts to matches
        settings.setMatchPostsTimeLimit(30); // Show posts from last 30 days to matches
        settings.setShowFollowersToMatches(false); // Don't show follower list to matches
        settings.setShowFollowingToMatches(false); // Don't show following list to matches

        return userPrivacySettingsRepository.save(settings);
    }

    /**
     * Update user's privacy settings
     */
    public UserPrivacySettings updatePrivacySettings(User user, UserPrivacySettings newSettings) {
        UserPrivacySettings existing = getUserPrivacySettings(user);

        // Update settings
        existing.setProfilePublic(newSettings.isProfilePublic());
        existing.setShowPostsToMatches(newSettings.isShowPostsToMatches());
        existing.setMaxPostsForMatches(newSettings.getMaxPostsForMatches());
        existing.setMatchPostsTimeLimit(newSettings.getMatchPostsTimeLimit());
        existing.setShowFollowersToMatches(newSettings.isShowFollowersToMatches());
        existing.setShowFollowingToMatches(newSettings.isShowFollowingToMatches());

        return userPrivacySettingsRepository.save(existing);
    }

    /**
     * Check if viewer can see user's follower/following lists
     */
    public boolean canViewFollowersList(User viewer, User profileOwner) {
        if (viewer.getId().equals(profileOwner.getId())) {
            return true; // Can always see your own lists
        }

        UserPrivacySettings privacy = getUserPrivacySettings(profileOwner);

        // If following, can see lists
        if (followRepository.existsByFollowerAndFollowing(viewer, profileOwner)) {
            return true;
        }

        // If matched, check match privacy settings
        if (areMatched(viewer, profileOwner)) {
            return privacy.isShowFollowersToMatches();
        }

        // Otherwise, only if profile is public
        return privacy.isProfilePublic();
    }

    /**
     * Check if viewer can see user's following list
     */
    public boolean canViewFollowingList(User viewer, User profileOwner) {
        if (viewer.getId().equals(profileOwner.getId())) {
            return true; // Can always see your own lists
        }

        UserPrivacySettings privacy = getUserPrivacySettings(profileOwner);

        // If following, can see lists
        if (followRepository.existsByFollowerAndFollowing(viewer, profileOwner)) {
            return true;
        }

        // If matched, check match privacy settings
        if (areMatched(viewer, profileOwner)) {
            return privacy.isShowFollowingToMatches();
        }

        // Otherwise, only if profile is public
        return privacy.isProfilePublic();
    }
}
