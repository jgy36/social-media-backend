package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.entity.Conversation;
import com.jgy36.PoliticalApp.entity.Message;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.repository.ConversationRepository;
import com.jgy36.PoliticalApp.repository.MessageRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Autowired
    public MessageService(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Get the current authenticated user
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    /**
     * Get all conversations for the current user
     */
    // Updated getUserConversations method in MessageService.java
    // Modify the getUserConversations method in MessageService.java
    public List<Map<String, Object>> getUserConversations() {
        User currentUser = getCurrentUser();
        List<Conversation> conversations = conversationRepository.findConversationsByParticipant(currentUser);

        return conversations.stream()
                .map(conversation -> {
                    // Get other participant(s)
                    List<User> otherParticipants = conversation.getParticipants().stream()
                            .filter(user -> !user.equals(currentUser))
                            .collect(Collectors.toList());

                    // Get the latest message
                    Message latestMessage = conversation.getLatestMessage();

                    // Count unread messages
                    int unreadCount = conversation.getUnreadCount(currentUser);

                    Map<String, Object> conversationData = new HashMap<>();
                    conversationData.put("id", conversation.getId());

                    // Extract other user for direct conversations - handle the case where there may not be exactly one other participant
                    if (otherParticipants.size() == 1) {
                        User otherUser = otherParticipants.get(0);
                        // Create a map that can handle null values
                        Map<String, Object> otherUserMap = new HashMap<>();
                        otherUserMap.put("id", otherUser.getId());
                        otherUserMap.put("username", otherUser.getUsername());
                        otherUserMap.put("displayName", otherUser.getDisplayName() != null ? otherUser.getDisplayName() : otherUser.getUsername());
                        otherUserMap.put("profileImageUrl", otherUser.getProfileImageUrl());  // This can be null

                        conversationData.put("otherUser", otherUserMap);
                    } else if (otherParticipants.isEmpty()) {
                        // Handle edge case: If somehow this is a self-conversation or broken conversation
                        // Create a map that can handle null values
                        Map<String, Object> otherUserMap = new HashMap<>();
                        otherUserMap.put("id", currentUser.getId());
                        otherUserMap.put("username", "Deleted User");
                        otherUserMap.put("displayName", "Deleted User");
                        otherUserMap.put("profileImageUrl", "");

                        conversationData.put("otherUser", otherUserMap);
                    } else {
                        // Group conversation case - for now, just use the first other participant
                        User otherUser = otherParticipants.get(0);
                        // Create a map that can handle null values
                        Map<String, Object> otherUserMap = new HashMap<>();
                        otherUserMap.put("id", otherUser.getId());
                        otherUserMap.put("username", otherUser.getUsername());
                        otherUserMap.put("displayName", otherUser.getDisplayName() != null ? otherUser.getDisplayName() : otherUser.getUsername());
                        otherUserMap.put("profileImageUrl", otherUser.getProfileImageUrl());  // This can be null

                        conversationData.put("otherUser", otherUserMap);
                    }

                    if (latestMessage != null) {
                        // Create a map that can handle null values
                        Map<String, Object> messageMap = new HashMap<>();
                        messageMap.put("id", latestMessage.getId());
                        messageMap.put("content", latestMessage.getContent());
                        messageMap.put("senderId", latestMessage.getSender() != null ? latestMessage.getSender().getId() : null);
                        messageMap.put("senderUsername", latestMessage.getSender() != null ? latestMessage.getSender().getUsername() : "Unknown");
                        messageMap.put("sentAt", latestMessage.getSentAt());
                        messageMap.put("read", latestMessage.isRead());

                        conversationData.put("latestMessage", messageMap);
                    }

                    conversationData.put("unreadCount", unreadCount);
                    conversationData.put("updatedAt", conversation.getUpdatedAt());

                    return conversationData;
                })
                .sorted(Comparator.comparing(
                        c -> c.containsKey("latestMessage")
                                ? ((LocalDateTime) ((Map<String, Object>) c.get("latestMessage")).get("sentAt"))
                                : LocalDateTime.MIN,
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Get or create a conversation with another user
     */
    @Transactional
    public Map<String, Object> getOrCreateConversation(Long otherUserId) {
        User currentUser = getCurrentUser();
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Optional<Conversation> existingConversation = conversationRepository.findDirectConversation(currentUser, otherUser);

        Conversation conversation;
        if (existingConversation.isPresent()) {
            conversation = existingConversation.get();
        } else {
            conversation = new Conversation(currentUser, otherUser);
            conversationRepository.save(conversation);
        }

        // Create a map that can handle null values for the other user
        Map<String, Object> otherUserMap = new HashMap<>();
        otherUserMap.put("id", otherUser.getId());
        otherUserMap.put("username", otherUser.getUsername());
        otherUserMap.put("displayName", otherUser.getDisplayName());  // This can be null
        otherUserMap.put("profileImageUrl", otherUser.getProfileImageUrl());  // This can be null

        // Create the result map
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("id", conversation.getId());
        resultMap.put("otherUser", otherUserMap);

        return resultMap;
    }

    /**
     * Get a conversation by ID
     */
    public Conversation getConversationById(Long conversationId) {
        User currentUser = getCurrentUser();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NoSuchElementException("Conversation not found"));

        // Check if the current user is a participant
        if (!conversation.getParticipants().contains(currentUser)) {
            throw new IllegalStateException("You are not a participant in this conversation");
        }

        return conversation;
    }

    /**
     * Get messages for a conversation
     */
    public List<Message> getMessagesForConversation(Conversation conversation) {
        return messageRepository.findByConversationOrderBySentAtAsc(conversation);
    }

    /**
     * Create a new conversation
     */
    @Transactional
    public Conversation createConversation(User user1, User user2) {
        Conversation conversation = new Conversation(user1, user2);
        return conversationRepository.save(conversation);
    }

    /**
     * Send a message in a conversation
     */
    @Transactional
    public Message sendMessage(Long conversationId, String content, String imageUrl) {
        User currentUser = getCurrentUser();
        Conversation conversation = getConversationById(conversationId);

        // Check if the current user is a participant
        if (!conversation.getParticipants().contains(currentUser)) {
            throw new IllegalStateException("You are not a participant in this conversation");
        }

        // Create and save the message
        Message message = new Message(conversation, currentUser, content);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            message.setImageUrl(imageUrl);
        }

        messageRepository.save(message);

        // Update conversation's updatedAt timestamp
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Send notifications to other participants
        conversation.getParticipants().stream()
                .filter(user -> !user.equals(currentUser))
                .forEach(recipient -> {
                    String notificationMessage = currentUser.getUsername() + " sent you a message: \"" +
                            (content.length() > 50 ? content.substring(0, 47) + "..." : content) + "\"";
                    notificationService.createNotification(recipient, notificationMessage);
                });

        return message;
    }

    /**
     * Mark all messages in a conversation as read
     */
    @Transactional
    public void markConversationAsRead(Conversation conversation, User currentUser) {
        List<Message> unreadMessages = messageRepository.findUnreadMessagesInConversation(conversation, currentUser);

        unreadMessages.forEach(message -> {
            message.setRead(true);
            messageRepository.save(message);
        });
    }

    /**
     * Count unread messages for a user
     */
    public long countUnreadMessagesForUser(User user) {
        return messageRepository.countUnreadMessagesForUser(user);
    }
}
