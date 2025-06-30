package com.jgy36.PoliticalApp.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CommunityDTO {
    private String id;              // slug for URLs
    private String name;            // display name
    private String description;     // community description
    private int members;            // count of members
    private String created;         // creation timestamp
    private List<String> rules;     // community rules
    private List<String> moderators;// usernames of moderators
    private String color;           // color code for styling
    private String banner;          // banner image URL
    private boolean isJoined;       // whether current user has joined
    private boolean isNotificationsOn; // whether notifications are enabled
}
