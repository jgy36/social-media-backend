package com.jgy36.PoliticalApp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.ROLE_USER;

    @Column(nullable = false)
    private boolean verified = false;

    @JsonIgnore
    private String verificationToken;

    // Email verification token expiration (added for settings)
    @JsonIgnore
    private LocalDateTime verificationTokenExpiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime tokenExpirationTime;
    private boolean emailVerified = false;

    // Getters & Setters
    @Getter
    @ManyToMany
    @JoinTable(
            name = "follows",
            joinColumns = @JoinColumn(name = "follower_id"),
            inverseJoinColumns = @JoinColumn(name = "following_id")
    )
    @JsonIgnoreProperties("following")
    private Set<User> following = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Post> savedPosts;

    // Profile display name
    @Column(nullable = true)
    private String displayName;

    // User bio/description
    @Column(columnDefinition = "TEXT", nullable = true)
    private String bio;

    // Path to profile image
    @Column(nullable = true)
    private String profileImageUrl;

    // Add to existing User.java
    private Boolean datingModeEnabled = false;
    private Boolean datingProfileComplete = false;
    private LocalDateTime lastActive;

    // Settings relationships

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private UserSecuritySettings securitySettings;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private UserNotificationPreferences notificationPreferences;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private UserPrivacySettings privacySettings;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ConnectedAccount> connectedAccounts = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<UserSession> sessions = new ArrayList<>();

    public User() {
        this.verificationToken = UUID.randomUUID().toString();
        this.verificationTokenExpiresAt = LocalDateTime.now().plusDays(1);
    }

    public User(String username, String email, String password, Role role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.verified = false;
        this.verificationToken = UUID.randomUUID().toString();
        this.verificationTokenExpiresAt = LocalDateTime.now().plusDays(1);
        this.createdAt = LocalDateTime.now();
    }

    public void follow(User user) {
        following.add(user);
    }

    public void unfollow(User user) {
        following.remove(user);
    }

    // For compatibility with new settings implementation
    public boolean isEmailVerified() {
        return verified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.verified = emailVerified;
    }

    // Getters and setters for new settings relationships
    public UserSecuritySettings getSecuritySettings() {
        return securitySettings;
    }

    public void setSecuritySettings(UserSecuritySettings securitySettings) {
        this.securitySettings = securitySettings;
    }

    public UserNotificationPreferences getNotificationPreferences() {
        return notificationPreferences;
    }

    public void setNotificationPreferences(UserNotificationPreferences notificationPreferences) {
        this.notificationPreferences = notificationPreferences;
    }

    public UserPrivacySettings getPrivacySettings() {
        return privacySettings;
    }

    public void setPrivacySettings(UserPrivacySettings privacySettings) {
        this.privacySettings = privacySettings;
    }

    public List<ConnectedAccount> getConnectedAccounts() {
        return connectedAccounts;
    }

    public void setConnectedAccounts(List<ConnectedAccount> connectedAccounts) {
        this.connectedAccounts = connectedAccounts;
    }

    public List<UserSession> getSessions() {
        return sessions;
    }

    public void setSessions(List<UserSession> sessions) {
        this.sessions = sessions;
    }

    public LocalDateTime getVerificationTokenExpiresAt() {
        return verificationTokenExpiresAt;
    }

    public void setVerificationTokenExpiresAt(LocalDateTime verificationTokenExpiresAt) {
        this.verificationTokenExpiresAt = verificationTokenExpiresAt;
    }

}
