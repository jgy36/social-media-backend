package com.jgy36.PoliticalApp.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// DTO for conversation list item
@Getter
@Setter
@NoArgsConstructor
public class ConversationDTO {
    private Long id;
    private UserSummaryDTO otherUser;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private int unreadCount;
    private boolean isOnline;
}
