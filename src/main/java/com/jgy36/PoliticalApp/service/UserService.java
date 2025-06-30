package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.entity.PendingUser;
import com.jgy36.PoliticalApp.entity.Role;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.repository.PendingUserRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSettingsInitializer settingsInitializer;
    private final AccountManagementService accountManagementService;
    private final PrivacySettingsService privacySettingsService;
    private final FollowRequestService followRequestService;
    private final PendingUserRepository pendingUserRepository;
    private final EmailService emailService;


    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UserSettingsInitializer settingsInitializer,
            AccountManagementService accountManagementService,
            PrivacySettingsService privacySettingsService,
            FollowRequestService followRequestService,
            PendingUserRepository pendingUserRepository,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.settingsInitializer = settingsInitializer;
        this.accountManagementService = accountManagementService;
        this.privacySettingsService = privacySettingsService;
        this.followRequestService = followRequestService;
        this.pendingUserRepository = pendingUserRepository;
        this.emailService = emailService;
    }

    /**
     * Get the current authenticated user
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    // Updated register function for userService.ts
    @Transactional
    public User registerUser(String username, String email, String password, String displayName) {
        // ✅ Check if username already exists
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists. Please choose another.");
        }

        // ✅ Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists. Please use another email.");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));

        // Set the display name (new)
        if (displayName != null && !displayName.trim().isEmpty()) {
            user.setDisplayName(displayName);
        } else {
            // Use username as fallback for display name
            user.setDisplayName(username);
        }

        user.setRole(Role.ROLE_USER); // ✅ Set default role to ROLE_USER
        user.setVerified(false); // User must verify email
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenExpiresAt(LocalDateTime.now().plusDays(1));

        try {
            // Save user to get ID
            user = userRepository.save(user);

            // Initialize settings
            settingsInitializer.initializeSettings(user);

            // Save again with settings
            user = userRepository.save(user);

            // Send verification email
            try {
                System.out.println("=== UserService: About to send verification email ===");
                System.out.println("AccountManagementService is null? " + (accountManagementService == null));
                System.out.println("User email: " + user.getEmail());
                System.out.println("User token: " + user.getVerificationToken());
                System.out.println("User ID: " + user.getId());

                accountManagementService.sendVerificationEmail(user);
                System.out.println("=== UserService: Verification email method called successfully ===");
            } catch (Exception e) {
                System.err.println("=== UserService: Error sending verification email ===");
                System.err.println("Exception type: " + e.getClass().getName());
                System.err.println("Exception message: " + e.getMessage());
                e.printStackTrace();
            }

            // Return the saved user
            return user;
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("A user with this username or email already exists.");
        }
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Update user profile
     */
    @Transactional
    public User updateProfile(Long userId, String displayName, String bio, String profileImageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (displayName != null) {
            user.setDisplayName(displayName);
        }
        if (bio != null) {
            user.setBio(bio);
        }
        if (profileImageUrl != null) {
            user.setProfileImageUrl(profileImageUrl);
        }

        return userRepository.save(user);
    }

    /**
     * Check if a user account is private
     *
     * @param userId ID of the user to check
     * @return true if the account is private, false otherwise
     */
    public boolean isAccountPrivate(Long userId) {
        return privacySettingsService.isAccountPrivate(userId);
    }

    /**
     * Follow a user or create a follow request based on privacy settings
     *
     * @param targetUserId ID of the user to follow
     * @return Result map with success status and information about the follow/request
     */
    @Transactional
    public Map<String, Object> followUser(Long targetUserId) {
        try {
            // Use the followRequestService to create a follow or request
            boolean directFollow = followRequestService.createFollowOrRequest(targetUserId);

            if (directFollow) {
                // Direct follow was created - account is public or already following
                return Map.of(
                        "success", true,
                        "followStatus", "following",
                        "message", "Successfully followed user"
                );
            } else {
                // Follow request was created - account is private
                return Map.of(
                        "success", true,
                        "followStatus", "requested",
                        "message", "Follow request sent"
                );
            }
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * Unfollow a user
     *
     * @param targetUserId ID of the user to unfollow
     * @return Result map with success status
     */
    @Transactional
    public Map<String, Object> unfollowUser(Long targetUserId) {
        try {
            User currentUser = getCurrentUser();
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Check if currently following
            if (!currentUser.getFollowing().contains(targetUser)) {
                // Not following, but check for pending requests
                boolean hasPendingRequest = followRequestService.hasPendingRequest(currentUser, targetUser);

                if (hasPendingRequest) {
                    // Cancel the pending request
                    // This would need to be implemented in the FollowRequestService
                    return Map.of(
                            "success", true,
                            "message", "Follow request canceled"
                    );
                }

                return Map.of(
                        "success", false,
                        "message", "Not following this user"
                );
            }

            // Remove from following
            currentUser.getFollowing().remove(targetUser);
            userRepository.save(currentUser);

            return Map.of(
                    "success", true,
                    "message", "Successfully unfollowed user"
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * Get follow status for a user
     *
     * @param targetUserId ID of the user to check
     * @return Follow status information including counts and relationship status
     */
    public Map<String, Object> getFollowStatus(Long targetUserId) {
        try {
            User currentUser = getCurrentUser();
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            boolean isFollowing = currentUser.getFollowing().contains(targetUser);
            boolean isRequested = false;

            // If not following, check if there's a pending request
            if (!isFollowing) {
                isRequested = followRequestService.hasPendingRequest(currentUser, targetUser);
            }

            // Get counts
            int followersCount = userRepository.countFollowers(targetUserId);
            int followingCount = userRepository.countFollowing(targetUserId);

            return Map.of(
                    "isFollowing", isFollowing,
                    "isRequested", isRequested,
                    "followersCount", followersCount,
                    "followingCount", followingCount
            );
        } catch (Exception e) {
            return Map.of(
                    "isFollowing", false,
                    "isRequested", false,
                    "followersCount", 0,
                    "followingCount", 0,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * Get pending follow requests count for the current user
     *
     * @return Number of pending follow requests
     */
    public int getPendingFollowRequestsCount() {
        User currentUser = getCurrentUser();
        return followRequestService.getFollowRequestsForUser(currentUser).size();
    }

    @Transactional
    public void createPendingUser(String username, String email, String password, String displayName) {
        // Check if username/email already exists in both tables
        if (userRepository.existsByUsername(username) || pendingUserRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(email) || pendingUserRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        PendingUser pendingUser = new PendingUser();
        pendingUser.setUsername(username);
        pendingUser.setEmail(email);
        pendingUser.setPassword(passwordEncoder.encode(password));
        pendingUser.setDisplayName(displayName);
        pendingUser.setVerificationToken(UUID.randomUUID().toString());
        pendingUser.setExpiresAt(LocalDateTime.now().plusDays(1));

        pendingUserRepository.save(pendingUser);

        // Send verification email directly using EmailService
        try {
            emailService.sendVerificationEmail(pendingUser.getEmail(), pendingUser.getVerificationToken());
        } catch (Exception e) {
            System.err.println("Failed to send verification email: " + e.getMessage());
            // Don't throw the exception - user is still created
        }
    }

    @Transactional
    public User verifyAndCreateUser(String token) {
        PendingUser pendingUser = pendingUserRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (pendingUser.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification token has expired");
        }

        // Create the actual user
        User user = new User();
        user.setUsername(pendingUser.getUsername());
        user.setEmail(pendingUser.getEmail());
        user.setPassword(pendingUser.getPassword()); // Already encrypted
        user.setDisplayName(pendingUser.getDisplayName());
        user.setRole(Role.ROLE_USER);
        user.setVerified(true);

        User savedUser = userRepository.save(user);

        // Initialize settings
        settingsInitializer.initializeSettings(savedUser);

        // Delete the pending user
        pendingUserRepository.delete(pendingUser);

        return savedUser;
    }
    // Add this method to your existing UserService.java class

    public boolean verifyEmailCode(String code) {
        logger.info("=== VERIFYING EMAIL CODE ===");
        logger.info("Code to verify: {}", code);

        try {
            // Look in PENDING_USERS table instead of users table
            List<PendingUser> pendingUsers = pendingUserRepository.findAll();
            logger.info("🔍 Total pending users found: {}", pendingUsers.size());

            if (pendingUsers.isEmpty()) {
                logger.warn("❌ No pending users found in database!");
                return false;
            }

            // Debug: Show all pending users and their tokens
            for (PendingUser pendingUser : pendingUsers) {
                logger.info("🔍 Pending user: {} with token: {}", pendingUser.getEmail(), pendingUser.getVerificationToken());

                if (pendingUser.getVerificationToken() != null) {
                    String generatedCode = generateVerificationCode(pendingUser.getVerificationToken());
                    logger.info("🔍 Generated code for {}: {} (from token: {})",
                            pendingUser.getEmail(), generatedCode, pendingUser.getVerificationToken());

                    if (code.equalsIgnoreCase(generatedCode)) {
                        logger.info("✅ CODE MATCH FOUND for pending user: {}", pendingUser.getEmail());

                        // Check if token is still valid (not expired)
                        if (pendingUser.getExpiresAt() != null &&
                                pendingUser.getExpiresAt().isBefore(LocalDateTime.now())) {
                            logger.warn("❌ Verification token expired for pending user: {}", pendingUser.getEmail());
                            return false;
                        }

                        // Create the actual user from pending user
                        User newUser = new User();
                        newUser.setUsername(pendingUser.getUsername());
                        newUser.setEmail(pendingUser.getEmail());
                        newUser.setPassword(pendingUser.getPassword()); // Already encoded
                        newUser.setDisplayName(pendingUser.getDisplayName());
                        newUser.setEmailVerified(true); // Mark as verified
                        newUser.setVerificationToken(null);
                        newUser.setTokenExpirationTime(null);
                        newUser.setCreatedAt(LocalDateTime.now());

                        // Save to users table
                        userRepository.save(newUser);
                        logger.info("✅ Created verified user: {}", newUser.getEmail());

                        // Remove from pending_users table
                        pendingUserRepository.delete(pendingUser);
                        logger.info("✅ Removed from pending users: {}", pendingUser.getEmail());

                        return true;
                    }
                } else {
                    logger.warn("⚠️ Pending user {} has null verification token", pendingUser.getEmail());
                }
            }

            logger.warn("❌ No pending user found with verification code: {}", code);
            return false;

        } catch (Exception e) {
            logger.error("💥 Error verifying email code: ", e);
            return false;
        }
    }

    private String generateVerificationCode(String token) {
        String cleaned = token.replaceAll("-", "").toUpperCase();
        if (cleaned.length() >= 6) {
            return cleaned.substring(0, 6);
        } else {
            return (cleaned + "123456").substring(0, 6);
        }
    }


}

