package com.jgy36.PoliticalApp.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// DTO for message responses
@Getter
@Setter
@NoArgsConstructor
public class MessageResponse {
    private Long id;
    private String content;
    private UserSummaryDTO sender;
    private UserSummaryDTO receiver;
    private Long conversationId;
    private LocalDateTime createdAt;
    private boolean read;
}
