package com.jgy36.PoliticalApp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "user_security_settings")
public class UserSecuritySettings {
    @Id
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private boolean twoFaEnabled;
    private String twoFaSecret;
    private LocalDateTime lastPasswordChange;

    // Constructors
    public UserSecuritySettings() {
    }

    public UserSecuritySettings(User user, boolean twoFaEnabled, String twoFaSecret, LocalDateTime lastPasswordChange) {
        this.user = user;
        this.twoFaEnabled = twoFaEnabled;
        this.twoFaSecret = twoFaSecret;
        this.lastPasswordChange = lastPasswordChange;
    }
}
