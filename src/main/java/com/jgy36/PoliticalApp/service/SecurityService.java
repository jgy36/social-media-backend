package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.entity.UserSecuritySettings;
import com.jgy36.PoliticalApp.entity.UserSession;
import com.jgy36.PoliticalApp.repository.UserRepository;
import com.jgy36.PoliticalApp.repository.UserSecuritySettingsRepository;
import com.jgy36.PoliticalApp.repository.UserSessionRepository;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SecurityService {
    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);
    private final UserSecuritySettingsRepository securityRepository;
    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public SecurityService(
            UserSecuritySettingsRepository securityRepository,
            UserRepository userRepository,
            UserSessionRepository sessionRepository,
            UserDetailsServiceImpl userDetailsService,
            PasswordEncoder passwordEncoder) {
        this.securityRepository = securityRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
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
     * Get user security settings
     */
    public UserSecuritySettings getUserSecuritySettings(Long userId) {
        return securityRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
                    UserSecuritySettings settings = new UserSecuritySettings();
                    settings.setUser(user);
                    settings.setTwoFaEnabled(false);
                    settings.setLastPasswordChange(LocalDateTime.now());
                    return securityRepository.save(settings);
                });
    }

    /**
     * Get current user's security settings
     */
    public UserSecuritySettings getCurrentUserSecuritySettings() {
        return getUserSecuritySettings(getCurrentUserId());
    }

    /**
     * Check if 2FA is enabled for a user
     */
    public boolean isTwoFaEnabled(Long userId) {
        return securityRepository.findByUserId(userId)
                .map(UserSecuritySettings::isTwoFaEnabled)
                .orElse(false);
    }

    /**
     * Generate a new 2FA secret
     */
    public String generateTwoFaSecret() {
        return Base32.random();
    }

    /**
     * Verify a 2FA code
     */
    public boolean verifyTwoFaCode(String secret, String code) {
        try {
            Totp totp = new Totp(secret);
            return totp.verify(code);
        } catch (Exception e) {
            logger.error("Error verifying TOTP code", e);
            return false;
        }
    }

    /**
     * Enable 2FA for a user
     */
    @Transactional
    public void enableTwoFa(Long userId, String secret) {
        UserSecuritySettings settings = getUserSecuritySettings(userId);
        settings.setTwoFaEnabled(true);
        settings.setTwoFaSecret(secret);
        securityRepository.save(settings);
    }

    /**
     * Disable 2FA for a user
     */
    @Transactional
    public void disableTwoFa(Long userId) {
        UserSecuritySettings settings = getUserSecuritySettings(userId);
        settings.setTwoFaEnabled(false);
        settings.setTwoFaSecret(null);
        securityRepository.save(settings);
    }

    /**
     * Change user's password
     */
    @Transactional
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Update last password change timestamp
        UserSecuritySettings settings = getUserSecuritySettings(userId);
        settings.setLastPasswordChange(LocalDateTime.now());
        securityRepository.save(settings);

        return true;
    }

    /**
     * Get active sessions for a user
     */
    public List<UserSession> getActiveSessions(Long userId) {
        return sessionRepository.findByUserId(userId);
    }

    /**
     * Create a new session for a user
     */
    @Transactional
    public UserSession createSession(Long userId, String browser, String os, String ipAddress, String location) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserSession session = new UserSession();
        session.setId(UUID.randomUUID().toString());
        session.setUser(user);
        session.setBrowser(browser);
        session.setOs(os);
        session.setIpAddress(ipAddress);
        session.setLocation(location);
        session.setLastActive(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(7)); // Set session to expire in 7 days

        return sessionRepository.save(session);
    }

    /**
     * Terminate a specific session
     */
    @Transactional
    public boolean terminateSession(Long userId, String sessionId) {
        Optional<UserSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent() && sessionOpt.get().getUser().getId().equals(userId)) {
            sessionRepository.deleteById(sessionId);
            return true;
        }
        return false;
    }

    /**
     * Terminate all sessions except the current one
     */
    @Transactional
    public void terminateAllSessionsExceptCurrent(Long userId, String currentSessionId) {
        sessionRepository.deleteAllExceptCurrentByUserId(userId, currentSessionId);
    }

    /**
     * Terminate all sessions for a user
     */
    @Transactional
    public void terminateAllSessions(Long userId) {
        sessionRepository.deleteAllByUserId(userId);
    }
}
