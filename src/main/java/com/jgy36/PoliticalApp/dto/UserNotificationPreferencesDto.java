package com.jgy36.PoliticalApp.dto;

import lombok.Getter;

// DTO for notification preferences
@Getter
public class UserNotificationPreferencesDto {
    private boolean emailNotifications = true;
    private boolean newCommentNotifications = true;
    private boolean mentionNotifications = true;
    private boolean politicalUpdates = false;
    private boolean communityUpdates = true;
    private boolean directMessageNotifications = true;
    private boolean followNotifications = true;
    private boolean likeNotifications = true;

    public UserNotificationPreferencesDto() {
    }

    public void setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public void setNewCommentNotifications(boolean newCommentNotifications) {
        this.newCommentNotifications = newCommentNotifications;
    }

    public void setMentionNotifications(boolean mentionNotifications) {
        this.mentionNotifications = mentionNotifications;
    }

    public void setPoliticalUpdates(boolean politicalUpdates) {
        this.politicalUpdates = politicalUpdates;
    }

    public void setCommunityUpdates(boolean communityUpdates) {
        this.communityUpdates = communityUpdates;
    }

    public void setDirectMessageNotifications(boolean directMessageNotifications) {
        this.directMessageNotifications = directMessageNotifications;
    }

    public void setFollowNotifications(boolean followNotifications) {
        this.followNotifications = followNotifications;
    }

    public void setLikeNotifications(boolean likeNotifications) {
        this.likeNotifications = likeNotifications;
    }
}
