package com.jgy36.PoliticalApp.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String username;
    private String displayName;  // Added this field
    private String bio;
    private String profileImageUrl;  // Added this field
    private String joinDate;
    private int followersCount;
    private int followingCount;
    private int postsCount;
    private boolean isFollowing;

    public void setIsFollowing(boolean isFollowing) {
        this.isFollowing = isFollowing;
    }
}
