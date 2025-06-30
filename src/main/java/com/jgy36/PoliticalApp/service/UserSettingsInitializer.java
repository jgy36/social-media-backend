package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.entity.UserNotificationPreferences;
import com.jgy36.PoliticalApp.entity.UserPrivacySettings;
import com.jgy36.PoliticalApp.entity.UserSecuritySettings;
import com.jgy36.PoliticalApp.repository.UserNotificationPreferencesRepository;
import com.jgy36.PoliticalApp.repository.UserPrivacySettingsRepository;
import com.jgy36.PoliticalApp.repository.UserSecuritySettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service to initialize settings for new users
 */
@Service
public class UserSettingsInitializer {
    private final UserSecuritySettingsRepository securityRepository;
    private final UserNotificationPreferencesRepository notificationRepository;
    private final UserPrivacySettingsRepository privacyRepository;

    public UserSettingsInitializer(
            UserSecuritySettingsRepository securityRepository,
            UserNotificationPreferencesRepository notificationRepository,
            UserPrivacySettingsRepository privacyRepository) {
        this.securityRepository = securityRepository;
        this.notificationRepository = notificationRepository;
        this.privacyRepository = privacyRepository;
    }

    /**
     * Initialize all settings for a new user
     */
    @Transactional
    public void initializeSettings(User user) {
        initializeSecuritySettings(user);
        initializeNotificationPreferences(user);
        initializePrivacySettings(user);
    }

    /**
     * Initialize security settings
     */
    @Transactional
    public void initializeSecuritySettings(User user) {
        if (user.getSecuritySettings() == null) {
            UserSecuritySettings settings = new UserSecuritySettings();
            settings.setUser(user);
            settings.setTwoFaEnabled(false);
            settings.setLastPasswordChange(LocalDateTime.now());
            securityRepository.save(settings);
            user.setSecuritySettings(settings);
        }
    }

    /**
     * Initialize notification preferences
     */
    @Transactional
    public void initializeNotificationPreferences(User user) {
        if (user.getNotificationPreferences() == null) {
            UserNotificationPreferences preferences = new UserNotificationPreferences();
            preferences.setUser(user);
            // Default values are already set in the entity
            notificationRepository.save(preferences);
            user.setNotificationPreferences(preferences);
        }
    }

    /**
     * Initialize privacy settings
     */
    @Transactional
    public void initializePrivacySettings(User user) {
        if (user.getPrivacySettings() == null) {
            UserPrivacySettings settings = new UserPrivacySettings();
            settings.setUser(user);
            // Default values are already set in the entity
            privacyRepository.save(settings);
            user.setPrivacySettings(settings);
        }
    }
}
