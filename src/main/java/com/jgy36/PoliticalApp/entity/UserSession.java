package com.jgy36.PoliticalApp.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_sessions")
public class UserSession {
    // Getters and Setters
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String browser;
    private String os;
    private String ipAddress;
    private String location;
    private LocalDateTime lastActive;
    private LocalDateTime expiresAt;

    // Constructors
    public UserSession() {
    }

    public UserSession(String id, User user, String browser, String os, String ipAddress, String location, LocalDateTime lastActive, LocalDateTime expiresAt) {
        this.id = id;
        this.user = user;
        this.browser = browser;
        this.os = os;
        this.ipAddress = ipAddress;
        this.location = location;
        this.lastActive = lastActive;
        this.expiresAt = expiresAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setLastActive(LocalDateTime lastActive) {
        this.lastActive = lastActive;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
