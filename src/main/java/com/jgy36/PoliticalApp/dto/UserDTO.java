package com.jgy36.PoliticalApp.dto;

public class UserDTO {
    private Long id;
    private String username;
    private String displayName;
    private String profileImageUrl;

    // Constructors
    public UserDTO() {
    }

    public UserDTO(Long id, String username, String displayName, String profileImageUrl) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.profileImageUrl = profileImageUrl;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
