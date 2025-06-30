package com.jgy36.PoliticalApp.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO for sending a new message
@Getter
@Setter
@NoArgsConstructor
public class MessageRequest {
    private String content;
    private Long receiverId;  // When starting a new conversation
    private Long conversationId;  // When replying in an existing conversation
}
