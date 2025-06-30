package com.jgy36.PoliticalApp.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "user_notification_preferences")
public class UserNotificationPreferences {
    // Getters and Setters
    @Id
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private boolean emailNotifications = true;
    private boolean newCommentNotifications = true;
    private boolean mentionNotifications = true;
    private boolean politicalUpdates = false;
    private boolean communityUpdates = true;
    private boolean directMessageNotifications = true;
    private boolean followNotifications = true;
    private boolean likeNotifications = true;

    // Constructors
    public UserNotificationPreferences() {
    }

    public UserNotificationPreferences(User user) {
        this.user = user;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUser(User user) {
        this.user = user;
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
