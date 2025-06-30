package com.jgy36.PoliticalApp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_privacy_settings")
public class UserPrivacySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Basic privacy settings
    @Column(name = "profile_public", nullable = false)
    private boolean profilePublic = true;

    @Column(name = "show_post_history", nullable = false)
    private boolean showPostHistory = true;

    @Column(name = "allow_direct_messages", nullable = false)
    private boolean allowDirectMessages = true;

    @Column(name = "allow_followers", nullable = false)
    private boolean allowFollowers = true;

    @Column(name = "allow_search_indexing", nullable = false)
    private boolean allowSearchIndexing = true;

    @Column(name = "data_sharing", nullable = false)
    private boolean dataSharing = false;

    // Dating app specific privacy settings
    @Column(name = "show_posts_to_matches", nullable = false)
    private boolean showPostsToMatches = true;

    @Column(name = "max_posts_for_matches")
    private Integer maxPostsForMatches = 10;

    @Column(name = "match_posts_time_limit")
    private Integer matchPostsTimeLimit = 30; // days

    @Column(name = "show_followers_to_matches", nullable = false)
    private boolean showFollowersToMatches = false;

    @Column(name = "show_following_to_matches", nullable = false)
    private boolean showFollowingToMatches = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public UserPrivacySettings() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UserPrivacySettings(User user) {
        this();
        this.user = user;
    }

    // Basic Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    // Profile Public Methods (supporting both naming conventions)
    public boolean isProfilePublic() {
        return profilePublic;
    }

    public boolean isPublicProfile() {
        return profilePublic;
    }

    public void setProfilePublic(boolean profilePublic) {
        this.profilePublic = profilePublic;
        this.updatedAt = LocalDateTime.now();
    }

    public void setPublicProfile(boolean publicProfile) {
        this.profilePublic = publicProfile;
        this.updatedAt = LocalDateTime.now();
    }

    // Show Post History Methods
    public boolean isShowPostHistory() {
        return showPostHistory;
    }

    public void setShowPostHistory(boolean showPostHistory) {
        this.showPostHistory = showPostHistory;
        this.updatedAt = LocalDateTime.now();
    }

    // Allow Direct Messages Methods
    public boolean isAllowDirectMessages() {
        return allowDirectMessages;
    }

    public void setAllowDirectMessages(boolean allowDirectMessages) {
        this.allowDirectMessages = allowDirectMessages;
        this.updatedAt = LocalDateTime.now();
    }

    // Allow Followers Methods
    public boolean isAllowFollowers() {
        return allowFollowers;
    }

    public void setAllowFollowers(boolean allowFollowers) {
        this.allowFollowers = allowFollowers;
        this.updatedAt = LocalDateTime.now();
    }

    // Allow Search Indexing Methods
    public boolean isAllowSearchIndexing() {
        return allowSearchIndexing;
    }

    public void setAllowSearchIndexing(boolean allowSearchIndexing) {
        this.allowSearchIndexing = allowSearchIndexing;
        this.updatedAt = LocalDateTime.now();
    }

    // Data Sharing Methods
    public boolean isDataSharing() {
        return dataSharing;
    }

    public void setDataSharing(boolean dataSharing) {
        this.dataSharing = dataSharing;
        this.updatedAt = LocalDateTime.now();
    }

    // Dating Features Methods
    public boolean isShowPostsToMatches() {
        return showPostsToMatches;
    }

    public void setShowPostsToMatches(boolean showPostsToMatches) {
        this.showPostsToMatches = showPostsToMatches;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getMaxPostsForMatches() {
        return maxPostsForMatches;
    }

    public void setMaxPostsForMatches(Integer maxPostsForMatches) {
        this.maxPostsForMatches = maxPostsForMatches;
        this.updatedAt = LocalDateTime.now();
    }

    public void setMaxPostsForMatches(int maxPostsForMatches) {
        this.maxPostsForMatches = maxPostsForMatches;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getMatchPostsTimeLimit() {
        return matchPostsTimeLimit;
    }

    public void setMatchPostsTimeLimit(Integer matchPostsTimeLimit) {
        this.matchPostsTimeLimit = matchPostsTimeLimit;
        this.updatedAt = LocalDateTime.now();
    }

    public void setMatchPostsTimeLimit(int matchPostsTimeLimit) {
        this.matchPostsTimeLimit = matchPostsTimeLimit;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isShowFollowersToMatches() {
        return showFollowersToMatches;
    }

    public void setShowFollowersToMatches(boolean showFollowersToMatches) {
        this.showFollowersToMatches = showFollowersToMatches;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isShowFollowingToMatches() {
        return showFollowingToMatches;
    }

    public void setShowFollowingToMatches(boolean showFollowingToMatches) {
        this.showFollowingToMatches = showFollowingToMatches;
        this.updatedAt = LocalDateTime.now();
    }

    // Timestamp Methods
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "UserPrivacySettings{" +
                "id=" + id +
                ", userId=" + getUserId() +
                ", profilePublic=" + profilePublic +
                ", showPostHistory=" + showPostHistory +
                ", allowDirectMessages=" + allowDirectMessages +
                ", allowFollowers=" + allowFollowers +
                ", allowSearchIndexing=" + allowSearchIndexing +
                ", dataSharing=" + dataSharing +
                ", showPostsToMatches=" + showPostsToMatches +
                ", maxPostsForMatches=" + maxPostsForMatches +
                ", matchPostsTimeLimit=" + matchPostsTimeLimit +
                ", showFollowersToMatches=" + showFollowersToMatches +
                ", showFollowingToMatches=" + showFollowingToMatches +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
