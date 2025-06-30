package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.dto.UserNotificationPreferencesDto;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.entity.UserNotificationPreferences;
import com.jgy36.PoliticalApp.repository.UserNotificationPreferencesRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPreferencesService {
    private final UserNotificationPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;

    public NotificationPreferencesService(UserNotificationPreferencesRepository preferencesRepository, UserRepository userRepository) {
        this.preferencesRepository = preferencesRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get the current authenticated user's ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return user.getId();
    }

    /**
     * Get notification preferences for a user
     * If preferences don't exist, create default ones
     */
    @Transactional
    public UserNotificationPreferences getPreferences(Long userId) {
        return preferencesRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
                    UserNotificationPreferences preferences = new UserNotificationPreferences(user);
                    return preferencesRepository.save(preferences);
                });
    }

    /**
     * Get current user's notification preferences
     */
    public UserNotificationPreferences getCurrentUserPreferences() {
        return getPreferences(getCurrentUserId());
    }

    /**
     * Update preferences for a user based on DTO
     */
    @Transactional
    public UserNotificationPreferences updatePreferences(Long userId, UserNotificationPreferencesDto preferencesDto) {
        UserNotificationPreferences preferences = getPreferences(userId);

        // Update fields from DTO
        preferences.setEmailNotifications(preferencesDto.isEmailNotifications());
        preferences.setNewCommentNotifications(preferencesDto.isNewCommentNotifications());
        preferences.setMentionNotifications(preferencesDto.isMentionNotifications());
        preferences.setPoliticalUpdates(preferencesDto.isPoliticalUpdates());
        preferences.setCommunityUpdates(preferencesDto.isCommunityUpdates());
        preferences.setDirectMessageNotifications(preferencesDto.isDirectMessageNotifications());
        preferences.setFollowNotifications(preferencesDto.isFollowNotifications());
        preferences.setLikeNotifications(preferencesDto.isLikeNotifications());

        return preferencesRepository.save(preferences);
    }

    /**
     * Update current user's notification preferences
     */
    @Transactional
    public UserNotificationPreferences updateCurrentUserPreferences(UserNotificationPreferencesDto preferencesDto) {
        return updatePreferences(getCurrentUserId(), preferencesDto);
    }

    /**
     * Reset preferences to default values
     */
    @Transactional
    public UserNotificationPreferences resetPreferences(Long userId) {
        UserNotificationPreferences preferences = getPreferences(userId);

        // Reset to default values
        preferences.setEmailNotifications(true);
        preferences.setNewCommentNotifications(true);
        preferences.setMentionNotifications(true);
        preferences.setPoliticalUpdates(false);
        preferences.setCommunityUpdates(true);
        preferences.setDirectMessageNotifications(true);
        preferences.setFollowNotifications(true);
        preferences.setLikeNotifications(true);

        return preferencesRepository.save(preferences);
    }

    /**
     * Reset current user's preferences to default values
     */
    @Transactional
    public UserNotificationPreferences resetCurrentUserPreferences() {
        return resetPreferences(getCurrentUserId());
    }

    /**
     * Convert entity to DTO
     */
    public UserNotificationPreferencesDto toDto(UserNotificationPreferences preferences) {
        UserNotificationPreferencesDto dto = new UserNotificationPreferencesDto();
        dto.setEmailNotifications(preferences.isEmailNotifications());
        dto.setNewCommentNotifications(preferences.isNewCommentNotifications());
        dto.setMentionNotifications(preferences.isMentionNotifications());
        dto.setPoliticalUpdates(preferences.isPoliticalUpdates());
        dto.setCommunityUpdates(preferences.isCommunityUpdates());
        dto.setDirectMessageNotifications(preferences.isDirectMessageNotifications());
        dto.setFollowNotifications(preferences.isFollowNotifications());
        dto.setLikeNotifications(preferences.isLikeNotifications());
        return dto;
    }
}
