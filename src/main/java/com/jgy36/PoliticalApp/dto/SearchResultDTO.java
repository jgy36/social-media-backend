package com.jgy36.PoliticalApp.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SearchResultDTO {
    private String id;
    private String type;         // "user", "community", "hashtag", "post"
    private String name;         // display name
    private String username;     // for users
    private String bio;          // for users
    private String description;  // for communities
    private String content;      // for posts
    private String author;       // for posts
    private String createdAt;    // for posts
    private Integer followersCount; // for users
    private Integer members;     // for communities
    private String tag;          // for hashtags (with #)
    private Integer count;       // for hashtags usage count
    private Integer postCount;   // for hashtags
}
