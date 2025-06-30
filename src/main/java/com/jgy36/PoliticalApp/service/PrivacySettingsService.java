package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.dto.UserPrivacySettingsDto;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.entity.UserPrivacySettings;
import com.jgy36.PoliticalApp.repository.UserPrivacySettingsRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PrivacySettingsService {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final UserPrivacySettingsRepository privacyRepository;
    private final UserRepository userRepository;

    public PrivacySettingsService(UserPrivacySettingsRepository privacyRepository, UserRepository userRepository) {
        this.privacyRepository = privacyRepository;
        this.userRepository = userRepository;
        logInfo("PrivacySettingsService initialized");
    }

    private void logInfo(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.println("[PRIVACY-SERVICE " + timestamp + "] " + message);
    }

    private void logError(String message, Throwable e) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.err.println("[PRIVACY-SERVICE ERROR " + timestamp + "] " + message);
        if (e != null) {
            e.printStackTrace();
        }
    }

    private Long getCurrentUserId() {
        logInfo("Getting current user ID from authentication context");
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            logInfo("Auth email: " + email);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            logInfo("Current user ID: " + user.getId() + ", username: " + user.getUsername());
            return user.getId();
        } catch (Exception e) {
            logError("Failed to get current user ID", e);
            throw e;
        }
    }

    public User getCurrentUser() {
        logInfo("Getting current user from authentication context");
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            logInfo("Auth email: " + email);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            logInfo("Found current user: ID=" + user.getId() + ", username=" + user.getUsername());
            return user;
        } catch (Exception e) {
            logError("Failed to get current user", e);
            throw e;
        }
    }

    @Transactional
    public UserPrivacySettings getSettings(Long userId) {
        logInfo("=== GET PRIVACY SETTINGS [userId=" + userId + "] ===");
        try {
            logInfo("Checking if privacy settings exist for user " + userId);
            var settingsOpt = privacyRepository.findByUserId(userId);

            if (settingsOpt.isPresent()) {
                UserPrivacySettings settings = settingsOpt.get();
                logInfo("Found existing settings: publicProfile=" + settings.isPublicProfile() +
                        ", allowFollowers=" + settings.isAllowFollowers() +
                        ", allowSearchIndexing=" + settings.isAllowSearchIndexing());
                return settings;
            } else {
                logInfo("No settings found, creating default settings");
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> {
                            logError("User not found with ID: " + userId, null);
                            return new UsernameNotFoundException("User not found with ID: " + userId);
                        });

                UserPrivacySettings settings = new UserPrivacySettings(user);
                logInfo("Created default settings: publicProfile=" + settings.isPublicProfile() +
                        ", allowFollowers=" + settings.isAllowFollowers() +
                        ", allowSearchIndexing=" + settings.isAllowSearchIndexing());

                UserPrivacySettings savedSettings = privacyRepository.save(settings);
                logInfo("Saved default settings with ID: " + savedSettings.getUserId());
                return savedSettings;
            }
        } catch (Exception e) {
            logError("Error getting privacy settings for user " + userId, e);
            throw e;
        } finally {
            logInfo("=== END GET PRIVACY SETTINGS ===");
        }
    }

    @Transactional
    public UserPrivacySettings getUserSettings(User user) {
        if (user == null) {
            logError("getUserSettings called with null user", null);
            throw new IllegalArgumentException("User cannot be null");
        }

        logInfo("=== GET USER SETTINGS [username=" + user.getUsername() + ", id=" + user.getId() + "] ===");
        try {
            var settingsOpt = privacyRepository.findByUserId(user.getId());

            if (settingsOpt.isPresent()) {
                UserPrivacySettings settings = settingsOpt.get();
                logInfo("Found existing settings: publicProfile=" + settings.isPublicProfile() +
                        ", allowFollowers=" + settings.isAllowFollowers() +
                        ", allowSearchIndexing=" + settings.isAllowSearchIndexing());
                return settings;
            } else {
                logInfo("Creating default privacy settings for user: " + user.getUsername());
                UserPrivacySettings settings = new UserPrivacySettings(user);
                logInfo("Default settings created: publicProfile=" + settings.isPublicProfile() +
                        ", allowFollowers=" + settings.isAllowFollowers() +
                        ", allowSearchIndexing=" + settings.isAllowSearchIndexing());

                UserPrivacySettings savedSettings = privacyRepository.save(settings);
                logInfo("Saved default settings with ID: " + savedSettings.getUserId());
                return savedSettings;
            }
        } catch (Exception e) {
            logError("Error getting user settings for user " + user.getId(), e);
            throw e;
        } finally {
            logInfo("=== END GET USER SETTINGS ===");
        }
    }

    public UserPrivacySettings getCurrentUserSettings() {
        logInfo("Getting privacy settings for current user");
        Long userId = getCurrentUserId();
        logInfo("Current user ID: " + userId);
        return getSettings(userId);
    }

    public boolean isAccountPrivate(Long userId) {
        logInfo("=============== CHECKING PRIVACY STATUS [userId=" + userId + "] ===============");
        try {
            String username = "unknown";
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    username = user.getUsername();
                }
            } catch (Exception e) {
                logError("Error getting username for user " + userId, e);
            }

            logInfo("Checking if account is private for user: " + username + " (ID: " + userId + ")");

            UserPrivacySettings settings = getSettings(userId);

            logInfo("Database record - Entity ID: " + settings.getUserId());
            logInfo("Database record - publicProfile: " + settings.isPublicProfile());
            logInfo("Database record - allowFollowers: " + settings.isAllowFollowers());
            logInfo("Database record - allowSearchIndexing: " + settings.isAllowSearchIndexing());

            boolean isPrivate = !settings.isPublicProfile();

            logInfo("PRIVACY CHECK RESULT: isPrivate=" + isPrivate);

            try {
                var freshSettings = privacyRepository.findByUserId(userId);
                if (freshSettings.isPresent()) {
                    logInfo("DOUBLE-CHECK - Fresh DB read: publicProfile=" + freshSettings.get().isPublicProfile() +
                            " (Should match: " + settings.isPublicProfile() + ")");
                } else {
                    logInfo("DOUBLE-CHECK - No settings found in fresh DB read!");
                }
            } catch (Exception e) {
                logError("Error performing double-check read", e);
            }

            return isPrivate;
        } catch (Exception e) {
            logError("Error checking if account is private for user " + userId, e);
            throw e;
        } finally {
            logInfo("=============== END PRIVACY CHECK ===============");
        }
    }

    public boolean isCurrentAccountPrivate() {
        logInfo("Checking if current user's account is private");
        Long userId = getCurrentUserId();
        logInfo("Current user ID: " + userId);
        return isAccountPrivate(userId);
    }

    @Transactional
    public UserPrivacySettings updateSettings(Long userId, UserPrivacySettingsDto settingsDto) {
        logInfo("=============== PRIVACY SETTINGS UPDATE [userId=" + userId + "] ===============");
        try {
            String username = "unknown";
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    username = user.getUsername();
                }
            } catch (Exception e) {
                logError("Error getting username for user " + userId, e);
            }

            logInfo("Updating privacy settings for user: " + username + " (ID: " + userId + ")");

            UserPrivacySettings settings = getSettings(userId);

            // Log current state in detail
            logInfo("CURRENT SETTINGS:");
            logInfo("  publicProfile = " + settings.isPublicProfile() + " (isPrivate = " + !settings.isPublicProfile() + ")");
            logInfo("  showPostHistory = " + settings.isShowPostHistory());
            logInfo("  allowDirectMessages = " + settings.isAllowDirectMessages());
            logInfo("  allowFollowers = " + settings.isAllowFollowers());
            logInfo("  allowSearchIndexing = " + settings.isAllowSearchIndexing());
            logInfo("  dataSharing = " + settings.isDataSharing());

            // Log new settings
            logInfo("NEW SETTINGS FROM DTO:");
            logInfo("  publicProfile = " + settingsDto.isPublicProfile() + " (isPrivate = " + !settingsDto.isPublicProfile() + ")");
            logInfo("  showPostHistory = " + settingsDto.isShowPostHistory());
            logInfo("  allowDirectMessages = " + settingsDto.isAllowDirectMessages());
            logInfo("  allowFollowers = " + settingsDto.isAllowFollowers());
            logInfo("  allowSearchIndexing = " + settingsDto.isAllowSearchIndexing());
            logInfo("  dataSharing = " + settingsDto.isDataSharing());

            // Track privacy setting change
            boolean wasPrivate = !settings.isPublicProfile();
            boolean willBePrivate = !settingsDto.isPublicProfile();

            logInfo("PRIVACY CHANGE: " + (wasPrivate ? "PRIVATE" : "PUBLIC") + " -> " +
                    (willBePrivate ? "PRIVATE" : "PUBLIC"));

            // Update basic fields from DTO
            settings.setPublicProfile(settingsDto.isPublicProfile());
            settings.setShowPostHistory(settingsDto.isShowPostHistory());
            settings.setAllowDirectMessages(settingsDto.isAllowDirectMessages());
            settings.setAllowFollowers(settingsDto.isAllowFollowers());
            settings.setDataSharing(settingsDto.isDataSharing());

            // Update dating-specific fields
            settings.setShowPostsToMatches(settingsDto.isShowPostsToMatches());
            settings.setMaxPostsForMatches(settingsDto.getMaxPostsForMatches());
            settings.setMatchPostsTimeLimit(settingsDto.getMatchPostsTimeLimit());
            settings.setShowFollowersToMatches(settingsDto.isShowFollowersToMatches());
            settings.setShowFollowingToMatches(settingsDto.isShowFollowingToMatches());

            // Enforce related settings for private accounts
            if (willBePrivate) {
                logInfo("Setting allowSearchIndexing=false because account is private");
                settings.setAllowSearchIndexing(false);
            } else {
                logInfo("Account is public, using user preference for search indexing: " +
                        settingsDto.isAllowSearchIndexing());
                settings.setAllowSearchIndexing(settingsDto.isAllowSearchIndexing());
            }

            // Save settings
            logInfo("Saving updated settings to database");
            UserPrivacySettings updatedSettings = privacyRepository.save(settings);

            // Log saved settings
            logInfo("SETTINGS AFTER SAVE:");
            logInfo("  publicProfile = " + updatedSettings.isPublicProfile() +
                    " (isPrivate = " + !updatedSettings.isPublicProfile() + ")");
            logInfo("  allowSearchIndexing = " + updatedSettings.isAllowSearchIndexing());
            logInfo("  allowFollowers = " + updatedSettings.isAllowFollowers());

            // Double-check settings are actually saved
            try {
                var freshSettings = privacyRepository.findByUserId(userId);
                if (freshSettings.isPresent()) {
                    logInfo("VERIFICATION - Fresh DB read: publicProfile=" + freshSettings.get().isPublicProfile() +
                            " (Should match: " + updatedSettings.isPublicProfile() + ")");
                } else {
                    logInfo("VERIFICATION - No settings found in fresh DB read!");
                }
            } catch (Exception e) {
                logError("Error verifying settings update", e);
            }

            if (wasPrivate != willBePrivate) {
                logInfo("!!!!! PRIVACY STATE CHANGED !!!!! From " +
                        (wasPrivate ? "PRIVATE" : "PUBLIC") + " to " +
                        (willBePrivate ? "PRIVATE" : "PUBLIC"));

                if (!wasPrivate && willBePrivate) {
                    logInfo("IMPORTANT: Account changed from PUBLIC to PRIVATE. Existing followers will need handling.");
                }
            }

            return updatedSettings;
        } catch (Exception e) {
            logError("Error updating privacy settings for user " + userId, e);
            throw e;
        } finally {
            logInfo("=============== END PRIVACY UPDATE ===============");
        }
    }

    @Transactional
    public UserPrivacySettings updateCurrentUserSettings(UserPrivacySettingsDto settingsDto) {
        logInfo("Updating current user's privacy settings");
        Long userId = getCurrentUserId();
        logInfo("Current user ID: " + userId);
        return updateSettings(userId, settingsDto);
    }

    @Transactional
    public UserPrivacySettings togglePrivateAccount() {
        logInfo("Toggling private account setting for current user");

        UserPrivacySettings settings = getCurrentUserSettings();
        boolean currentlyPrivate = !settings.isPublicProfile();
        logInfo("Current privacy state: " + (currentlyPrivate ? "PRIVATE" : "PUBLIC"));

        UserPrivacySettingsDto dto = toDto(settings);
        dto.setPublicProfile(currentlyPrivate); // Invert current privacy state

        boolean newPrivacyState = !dto.isPublicProfile();
        logInfo("New privacy state after toggle: " + (newPrivacyState ? "PRIVATE" : "PUBLIC"));

        if (newPrivacyState) {
            logInfo("Account will be private, setting allowSearchIndexing=false");
            dto.setAllowSearchIndexing(false);
        }

        return updateCurrentUserSettings(dto);
    }

    @Transactional
    public UserPrivacySettings setAccountPrivacy(Long userId, boolean isPrivate) {
        logInfo("Setting account privacy for user " + userId + " to " + (isPrivate ? "PRIVATE" : "PUBLIC"));

        UserPrivacySettings settings = getSettings(userId);
        UserPrivacySettingsDto dto = toDto(settings);

        boolean currentlyPrivate = !settings.isPublicProfile();
        logInfo("Current privacy state: " + (currentlyPrivate ? "PRIVATE" : "PUBLIC"));

        dto.setPublicProfile(!isPrivate);
        logInfo("New publicProfile value: " + dto.isPublicProfile());

        if (isPrivate) {
            logInfo("Account will be private, setting allowSearchIndexing=false");
            dto.setAllowSearchIndexing(false);
        }

        return updateSettings(userId, dto);
    }

    @Transactional
    public UserPrivacySettings resetSettings(Long userId) {
        logInfo("=============== RESETTING PRIVACY SETTINGS [userId=" + userId + "] ===============");
        try {
            UserPrivacySettings settings = getSettings(userId);

            logInfo("Current settings before reset:");
            logInfo("  publicProfile = " + settings.isPublicProfile());
            logInfo("  showPostHistory = " + settings.isShowPostHistory());
            logInfo("  allowDirectMessages = " + settings.isAllowDirectMessages());
            logInfo("  allowFollowers = " + settings.isAllowFollowers());
            logInfo("  allowSearchIndexing = " + settings.isAllowSearchIndexing());
            logInfo("  dataSharing = " + settings.isDataSharing());

            // Reset to default values - REMOVED POLITICAL FIELDS
            logInfo("Resetting to default values");
            settings.setPublicProfile(true); // Not private by default
            settings.setShowPostHistory(true);
            settings.setAllowDirectMessages(true);
            settings.setAllowFollowers(true);
            settings.setAllowSearchIndexing(true);
            settings.setDataSharing(false);

            // Reset dating features to defaults
            settings.setShowPostsToMatches(true);
            settings.setMaxPostsForMatches(10);
            settings.setMatchPostsTimeLimit(30);
            settings.setShowFollowersToMatches(false);
            settings.setShowFollowingToMatches(false);

            logInfo("Saving reset settings");
            UserPrivacySettings savedSettings = privacyRepository.save(settings);

            logInfo("Settings after reset and save:");
            logInfo("  publicProfile = " + savedSettings.isPublicProfile());
            logInfo("  allowSearchIndexing = " + savedSettings.isAllowSearchIndexing());

            return savedSettings;
        } catch (Exception e) {
            logError("Error resetting privacy settings for user " + userId, e);
            throw e;
        } finally {
            logInfo("=============== END RESET PRIVACY SETTINGS ===============");
        }
    }

    @Transactional
    public UserPrivacySettings resetCurrentUserSettings() {
        logInfo("Resetting current user's privacy settings to defaults");
        Long userId = getCurrentUserId();
        logInfo("Current user ID: " + userId);
        return resetSettings(userId);
    }

    public UserPrivacySettingsDto toDto(UserPrivacySettings settings) {
        logInfo("Converting UserPrivacySettings to DTO for user " + settings.getUserId());

        UserPrivacySettingsDto dto = new UserPrivacySettingsDto();

        // Basic settings
        dto.setPublicProfile(settings.isPublicProfile());
        dto.setShowPostHistory(settings.isShowPostHistory());
        dto.setAllowDirectMessages(settings.isAllowDirectMessages());
        dto.setAllowFollowers(settings.isAllowFollowers());
        dto.setAllowSearchIndexing(settings.isAllowSearchIndexing());
        dto.setDataSharing(settings.isDataSharing());

        // Dating features
        dto.setShowPostsToMatches(settings.isShowPostsToMatches());
        dto.setMaxPostsForMatches(settings.getMaxPostsForMatches());
        dto.setMatchPostsTimeLimit(settings.getMatchPostsTimeLimit());
        dto.setShowFollowersToMatches(settings.isShowFollowersToMatches());
        dto.setShowFollowingToMatches(settings.isShowFollowingToMatches());

        logInfo("DTO created with publicProfile=" + dto.isPublicProfile() +
                " (isPrivate=" + !dto.isPublicProfile() + ")");

        return dto;
    }

    public UserPrivacySettingsDto getSimplifiedSettings(Long userId) {
        logInfo("Getting simplified privacy settings for user " + userId);

        UserPrivacySettings settings = getSettings(userId);
        UserPrivacySettingsDto dto = new UserPrivacySettingsDto();
        dto.setPublicProfile(settings.isPublicProfile());

        logInfo("Simplified settings: publicProfile=" + dto.isPublicProfile() +
                " (isPrivate=" + !dto.isPublicProfile() + ")");

        return dto;
    }
}
