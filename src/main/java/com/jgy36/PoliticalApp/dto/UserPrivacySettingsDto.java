package com.jgy36.PoliticalApp.dto;

import lombok.Getter;

// DTO for privacy settings - Updated for social media/dating app
@Getter
public class UserPrivacySettingsDto {
    // Basic profile settings
    private boolean publicProfile = true;
    private boolean showPostHistory = true;
    private boolean allowDirectMessages = true;
    private boolean allowFollowers = true;
    private boolean allowSearchIndexing = true;
    private boolean dataSharing = false;

    // Dating app specific settings
    private boolean showPostsToMatches = true;
    private Integer maxPostsForMatches = 10;
    private Integer matchPostsTimeLimit = 30; // days
    private boolean showFollowersToMatches = false;
    private boolean showFollowingToMatches = false;

    public UserPrivacySettingsDto() {
    }

    // Basic profile setters
    public void setPublicProfile(boolean publicProfile) {
        this.publicProfile = publicProfile;
    }

    public void setShowPostHistory(boolean showPostHistory) {
        this.showPostHistory = showPostHistory;
    }

    public void setAllowDirectMessages(boolean allowDirectMessages) {
        this.allowDirectMessages = allowDirectMessages;
    }

    public void setAllowFollowers(boolean allowFollowers) {
        this.allowFollowers = allowFollowers;
    }

    public void setAllowSearchIndexing(boolean allowSearchIndexing) {
        this.allowSearchIndexing = allowSearchIndexing;
    }

    public void setDataSharing(boolean dataSharing) {
        this.dataSharing = dataSharing;
    }

    // Dating app setters
    public void setShowPostsToMatches(boolean showPostsToMatches) {
        this.showPostsToMatches = showPostsToMatches;
    }

    public void setMaxPostsForMatches(Integer maxPostsForMatches) {
        this.maxPostsForMatches = maxPostsForMatches;
    }

    public void setMatchPostsTimeLimit(Integer matchPostsTimeLimit) {
        this.matchPostsTimeLimit = matchPostsTimeLimit;
    }

    public void setShowFollowersToMatches(boolean showFollowersToMatches) {
        this.showFollowersToMatches = showFollowersToMatches;
    }

    public void setShowFollowingToMatches(boolean showFollowingToMatches) {
        this.showFollowingToMatches = showFollowingToMatches;
    }

    // Helper method to check if account is private (inverted publicProfile)
    public boolean isPrivateAccount() {
        return !publicProfile;
    }
}
