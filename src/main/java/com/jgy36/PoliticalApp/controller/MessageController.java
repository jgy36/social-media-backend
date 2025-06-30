package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.dto.ConversationDTO;
import com.jgy36.PoliticalApp.dto.MessageRequest;
import com.jgy36.PoliticalApp.dto.MessageResponse;
import com.jgy36.PoliticalApp.dto.UserSummaryDTO;
import com.jgy36.PoliticalApp.entity.Conversation;
import com.jgy36.PoliticalApp.entity.Message;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.repository.UserRepository;
import com.jgy36.PoliticalApp.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final UserRepository userRepository;

    public MessageController(MessageService messageService, UserRepository userRepository) {
        this.messageService = messageService;
        this.userRepository = userRepository;
    }

    /**
     * Get all conversations for the current user
     */
    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConversationDTO>> getUserConversations() {
        List<Map<String, Object>> conversations = messageService.getUserConversations();
        // Convert the Map to ConversationDTO objects
        List<ConversationDTO> conversationDTOs = conversations.stream()
                .map(this::convertToConversationDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(conversationDTOs);
    }

    // Updated convertToConversationDTO method for MessageController.java
    private ConversationDTO convertToConversationDTO(Map<String, Object> map) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId((Long) map.get("id"));

        // Properly convert otherUser data
        if (map.containsKey("otherUser")) {
            Map<String, Object> otherUserMap = (Map<String, Object>) map.get("otherUser");
            if (otherUserMap != null) {
                UserSummaryDTO otherUser = new UserSummaryDTO();
                otherUser.setId((Long) otherUserMap.get("id"));
                otherUser.setUsername((String) otherUserMap.get("username"));
                otherUser.setDisplayName((String) otherUserMap.get("displayName"));
                otherUser.setProfileImageUrl((String) otherUserMap.get("profileImageUrl"));
                dto.setOtherUser(otherUser);
            }
        }

        // Set latest message data
        if (map.containsKey("latestMessage")) {
            Map<String, Object> messageMap = (Map<String, Object>) map.get("latestMessage");
            if (messageMap != null) {
                dto.setLastMessage((String) messageMap.get("content"));
                if (messageMap.containsKey("sentAt")) {
                    dto.setLastMessageTime((LocalDateTime) messageMap.get("sentAt"));
                }
            }
        }

        // Set unread count and updated timestamp
        dto.setUnreadCount((Integer) map.getOrDefault("unreadCount", 0));

        // Default isOnline to false if not present
        dto.setOnline(false);

        return dto;
    }

    /**
     * Get or create a conversation with another user
     */
    @PostMapping("/conversations/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getOrCreateConversation(@PathVariable Long userId) {
        Map<String, Object> result = messageService.getOrCreateConversation(userId);
        Long conversationId = (Long) result.get("id");
        return ResponseEntity.ok(Map.of("conversationId", conversationId));
    }

    /**
     * Get all messages in a conversation
     */
    @GetMapping("/conversations/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MessageResponse>> getConversationMessages(@PathVariable Long conversationId) {
        // Get the current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // Get the conversation
        Conversation conversation = messageService.getConversationById(conversationId);

        // Get and convert messages
        List<Message> messages = messageService.getMessagesForConversation(conversation);
        List<MessageResponse> response = messages.stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());

        // Mark conversation as read
        messageService.markConversationAsRead(conversation, currentUser);

        return ResponseEntity.ok(response);
    }

    // Helper method to convert Message to MessageResponse
    private MessageResponse convertToMessageResponse(Message message) {
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setContent(message.getContent());
        response.setCreatedAt(message.getSentAt());
        response.setRead(message.isRead());

        // Add sender information
        if (message.getSender() != null) {
            UserSummaryDTO sender = new UserSummaryDTO();
            sender.setId(message.getSender().getId());
            sender.setUsername(message.getSender().getUsername());
            sender.setDisplayName(message.getSender().getDisplayName());
            sender.setProfileImageUrl(message.getSender().getProfileImageUrl());
            response.setSender(sender);
        }

        // Add conversation ID
        response.setConversationId(message.getConversation().getId());

        return response;
    }

    /**
     * Send a message to an existing conversation
     */
    @PostMapping("/conversations/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody MessageRequest request) {

        Message message = messageService.sendMessage(conversationId, request.getContent(), null);
        MessageResponse response = convertToMessageResponse(message);
        return ResponseEntity.ok(response);
    }

    /**
     * Start a new conversation with a user
     */
    @PostMapping("/new")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> startConversation(@RequestBody MessageRequest request) {
        if (request.getReceiverId() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Get the current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // Get the receiver
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new IllegalStateException("Receiver not found"));

        // Create conversation and send message
        Conversation conversation = messageService.createConversation(currentUser, receiver);
        Message message = messageService.sendMessage(conversation.getId(), request.getContent(), null);

        MessageResponse response = convertToMessageResponse(message);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the count of unread messages
     */
    @GetMapping("/unread/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getUnreadMessagesCount() {
        // Get the current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // Get unread count
        long count = messageService.countUnreadMessagesForUser(currentUser);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Mark all messages in a conversation as read
     */
    @PostMapping("/conversations/{conversationId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markConversationAsRead(@PathVariable Long conversationId) {
        // Get the current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // Get the conversation
        Conversation conversation = messageService.getConversationById(conversationId);

        // Mark as read
        messageService.markConversationAsRead(conversation, currentUser);
        return ResponseEntity.ok().build();
    }
}
