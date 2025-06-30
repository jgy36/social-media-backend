package com.jgy36.PoliticalApp.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Lightweight user info for messages
@Getter
@Setter
@NoArgsConstructor
public class UserSummaryDTO {
    private Long id;
    private String username;
    private String displayName;
    private String profileImageUrl;
}
