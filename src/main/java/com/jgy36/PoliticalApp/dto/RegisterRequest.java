package com.jgy36.PoliticalApp.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for registration requests.
 */
@Getter
@Setter
public class RegisterRequest {

    // Username
    @Getter
    private String username;
    // Email Address
    @Getter
    private String email;
    // Password
    @Getter
    private String password;
    private String displayName; // Add this field


    // ✅ Default Constructor (Needed for JSON deserialization)
    public RegisterRequest() {
    }

    // ✅ Parameterized Constructor (Useful for testing & manual object creation)
    public RegisterRequest(String username, String email, String password, String displayName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.displayName = displayName;

    }

    // ✅ Getters and Setters

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
