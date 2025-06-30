package com.jgy36.PoliticalApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {
    private String content;
    private Long originalPostId; // ID of the original post (for reposts)
    private boolean repost = false; // Changed from isRepost to repost for proper JSON mapping
    private Long communityId; // Optional community ID

    // Add explicit getter for repost flag with isRepost name
    public boolean isRepost() {
        return repost;
    }

    // Add explicit setter for repost flag
    public void setRepost(boolean repost) {
        this.repost = repost;
    }
}
