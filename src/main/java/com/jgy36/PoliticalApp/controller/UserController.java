package com.jgy36.PoliticalApp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jgy36.PoliticalApp.dto.PostDTO;
import com.jgy36.PoliticalApp.dto.UserProfileDTO;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.entity.UserPrivacySettings;
import com.jgy36.PoliticalApp.exception.ResourceNotFoundException;
import com.jgy36.PoliticalApp.repository.UserRepository;
import com.jgy36.PoliticalApp.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final FollowService followService;
    private final PostService postService;
    private final FollowRequestService followRequestService;
    private final UserService userService;
    private final PrivacySettingsService privacySettingsService;
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public UserController(UserRepository userRepository, FollowService followService, PostService postService, FollowRequestService followRequestService, UserService userService, PrivacySettingsService privacySettingsService) {
        this.userRepository = userRepository;
        this.followService = followService;
        this.postService = postService;
        this.followRequestService = followRequestService;
        this.userService = userService;
        this.privacySettingsService = privacySettingsService;
    }

    /**
     * ✅ Get the currently logged-in user's profile.
     * 🔒 Requires a valid JWT token.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName(); // Extract email from token

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        User user = userOpt.get();


        // Create a detailed response with all profile fields
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("displayName", user.getDisplayName());
        response.put("bio", user.getBio());
        response.put("profileImageUrl", user.getProfileImageUrl());
        response.put("createdAt", user.getCreatedAt().toString());

        // Get followers/following counts
        int followersCount = userRepository.countFollowers(user.getId());
        int followingCount = userRepository.countFollowing(user.getId());
        int postsCount = userRepository.countPosts(user.getId());

        response.put("followersCount", followersCount);
        response.put("followingCount", followingCount);
        response.put("postsCount", postsCount);

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Get a user's profile by username
     * This endpoint is now public for better user experience
     */
    @GetMapping("/profile/{username}")
    public ResponseEntity<?> getUserProfile(@PathVariable String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();

        // Get the current authenticated user if available
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = null;
        boolean isFollowing = false;

        if (authentication != null && !authentication.getName().equals("anonymousUser")) {
            Optional<User> currentUserOpt = userRepository.findByEmail(authentication.getName());
            if (currentUserOpt.isPresent()) {
                currentUser = currentUserOpt.get();
                isFollowing = currentUser.getFollowing().contains(user);
            }
        }

        // Get user stats
        int followersCount = followService.getFollowerCount(user.getId());
        int followingCount = followService.getFollowingCount(user.getId());
        int postsCount = followService.getPostCount(user.getId());

        // Create user profile DTO with necessary information
        UserProfileDTO profileDTO = new UserProfileDTO();
        profileDTO.setId(user.getId());
        profileDTO.setUsername(user.getUsername());
        profileDTO.setDisplayName(user.getDisplayName());  // Add this
        profileDTO.setBio(user.getBio());
        profileDTO.setProfileImageUrl(user.getProfileImageUrl());  // Add this
        profileDTO.setJoinDate(user.getCreatedAt().toString());
        profileDTO.setFollowersCount(followersCount);
        profileDTO.setFollowingCount(followingCount);
        profileDTO.setPostsCount(postsCount);
        profileDTO.setIsFollowing(isFollowing);

        return ResponseEntity.ok(profileDTO);
    }

    @GetMapping("/profile/{username}/posts")
    public ResponseEntity<?> getUserPosts(@PathVariable String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();

        // Get current user (if authenticated)
        User currentUser = null;
        try {
            currentUser = userService.getCurrentUser();
        } catch (Exception e) {
            // User not authenticated
        }

        // Check privacy - critical check that might be missing
        boolean isPrivate = privacySettingsService.isAccountPrivate(user.getId());
        boolean isOwner = currentUser != null && currentUser.getId().equals(user.getId());
        boolean isFollowing = currentUser != null && currentUser.getFollowing().contains(user);

        // If private and not owner and not following, don't show posts
        if (isPrivate && !isOwner && !isFollowing) {
            return ResponseEntity.ok(Collections.emptyList()); // Return empty list of posts
        }

        // This line has changed - we're now directly getting PostDTOs from the service
        List<PostDTO> postDTOs = postService.getPostsByUserId(user.getId());

        // Debug the response before sending
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            String jsonResponse = mapper.writeValueAsString(postDTOs);
            System.out.println("DEBUG: JSON response length: " + jsonResponse.length());
            System.out.println("DEBUG: First 100 chars: " +
                    (jsonResponse.length() > 100 ? jsonResponse.substring(0, 100) + "..." : jsonResponse));
        } catch (Exception e) {
            System.err.println("DEBUG: Error serializing response: " + e.getMessage());
        }
        // No need to convert to DTOs anymore since the service already returns them
        return ResponseEntity.ok(postDTOs);
    }

    /**
     * ✅ Follow a user
     */
    @PostMapping("/follow/{username}")
    public ResponseEntity<?> followUser(@PathVariable String username) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        Optional<User> currentUserOpt = userRepository.findByEmail(authentication.getName());
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Current user not found"));
        }

        Optional<User> targetUserOpt = userRepository.findByUsername(username);
        if (targetUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Target user not found"));
        }

        User currentUser = currentUserOpt.get();
        User targetUser = targetUserOpt.get();

        // Don't allow following yourself
        if (currentUser.getId().equals(targetUser.getId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Cannot follow yourself"));
        }

        try {
            // Use the followRequestService to create a follow or request based on privacy settings
            boolean directFollow = followService.createFollowOrRequest(targetUser.getId());

            // Get updated stats
            int followersCount = followService.getFollowerCount(targetUser.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            if (directFollow) {
                response.put("message", "Successfully followed user");
                response.put("followStatus", "following");
            } else {
                response.put("message", "Follow request sent");
                response.put("followStatus", "requested");
            }
            response.put("followersCount", followersCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ Unfollow a user
     */
    @DeleteMapping("/unfollow/{username}")
    public ResponseEntity<?> unfollowUser(@PathVariable String username) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        Optional<User> currentUserOpt = userRepository.findByEmail(authentication.getName());
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Current user not found"));
        }

        Optional<User> targetUserOpt = userRepository.findByUsername(username);
        if (targetUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Target user not found"));
        }

        User currentUser = currentUserOpt.get();
        User targetUser = targetUserOpt.get();

        // Remove target user from current user's following list
        currentUser.unfollow(targetUser);
        userRepository.save(currentUser);

        // Get updated stats
        int followersCount = followService.getFollowerCount(targetUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Successfully unfollowed user");
        response.put("followersCount", followersCount);

        return ResponseEntity.ok(response);
    }

    /**
     * Search for users by username
     *
     * @param query The search query to match against usernames
     * @return List of users matching the search query
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserProfileDTO>> searchUsers(@RequestParam String query) {
        // Find users with username containing the query (case insensitive)
        List<User> users = userRepository.findByUsernameContainingIgnoreCase(query);

        // Convert to UserProfileDTO objects
        List<UserProfileDTO> userDTOs = users.stream()
                .map(user -> {
                    UserProfileDTO dto = new UserProfileDTO();
                    dto.setId(user.getId());
                    dto.setUsername(user.getUsername());
                    dto.setJoinDate(user.getCreatedAt().toString());

                    // Get follower count
                    int followersCount = followService.getFollowerCount(user.getId());
                    int followingCount = followService.getFollowingCount(user.getId());
                    int postsCount = followService.getPostCount(user.getId());

                    dto.setFollowersCount(followersCount);
                    dto.setFollowingCount(followingCount);
                    dto.setPostsCount(postsCount);

                    // Check if current user is following this user
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    boolean isFollowing = false;

                    if (authentication != null && !authentication.getName().equals("anonymousUser")) {
                        Optional<User> currentUserOpt = userRepository.findByEmail(authentication.getName());
                        if (currentUserOpt.isPresent()) {
                            User currentUser = currentUserOpt.get();
                            isFollowing = currentUser.getFollowing().contains(user);
                        }
                    }

                    dto.setIsFollowing(isFollowing);

                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(userDTOs);
    }

    /**
     * ✅ Update the current user's username
     * This allows users to change their username while maintaining proper format requirements
     */
    @PutMapping("/update-username")
    public ResponseEntity<?> updateUsername(@RequestBody Map<String, String> request) {
        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "User not found"));
        }

        User user = userOpt.get();
        String newUsername = request.get("username");

        // Validate username
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Username cannot be empty"));
        }

        // Check username format (3-20 chars, alphanumeric, hyphens, underscores)
        Pattern usernamePattern = Pattern.compile("^[a-zA-Z0-9_-]{3,20}$");
        if (!usernamePattern.matcher(newUsername).matches()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false,
                            "message", "Username must be 3-20 characters and can only contain letters, numbers, underscores, and hyphens"));
        }

        // Check if username exists (case insensitive)
        if (userRepository.existsByUsernameIgnoreCase(newUsername) &&
                !user.getUsername().equalsIgnoreCase(newUsername)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "Username already taken"));
        }

        // Update username
        user.setUsername(newUsername);
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Username updated successfully");
        response.put("username", newUsername);

        return ResponseEntity.ok(response);
    }

    /**
     * Update the current user's profile information with enhanced logging
     */
    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) MultipartFile profileImage) {

        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        System.out.println("✅ Updating profile for user: " + email);

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            System.out.println("❌ User not found with email: " + email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "User not found"));
        }

        User user = userOpt.get();
        System.out.println("📋 Profile update request for user ID: " + user.getId());
        boolean updated = false;

        // Update display name if provided
        if (displayName != null) {
            String oldDisplayName = user.getDisplayName();
            user.setDisplayName(displayName);
            updated = true;
            System.out.println("✏️ Updated display name: '" + oldDisplayName + "' -> '" + displayName + "'");
        }

        // Update bio if provided
        if (bio != null) {
            String oldBio = user.getBio();
            user.setBio(bio);
            updated = true;
            System.out.println("✏️ Updated bio: '" + oldBio + "' -> '" + bio + "'");
        }

        // Handle profile image upload if provided
        String profileImageUrl = null;
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                // Use application's current working directory
                String currentDir = System.getProperty("user.dir");
                String uploadDir = currentDir + "/uploads/profile-images";

                System.out.println("📂 Current working directory: " + currentDir);

                File directory = new File(uploadDir);
                if (!directory.exists()) {
                    boolean created = directory.mkdirs();
                    System.out.println("📁 Created directory: " + uploadDir + " - " + (created ? "Success" : "Failed"));

                    if (!created) {
                        System.out.println("⚠️ Directory creation failed. Checking parent directories...");
                        File parent = new File(currentDir + "/uploads");
                        if (!parent.exists()) {
                            boolean parentCreated = parent.mkdir();
                            System.out.println("📁 Created parent directory: " + parent.getAbsolutePath() + " - " + (parentCreated ? "Success" : "Failed"));
                        }

                        // Try again
                        created = directory.mkdir();
                        System.out.println("📁 Second attempt to create directory: " + uploadDir + " - " + (created ? "Success" : "Failed"));
                    }
                }

                // Output directory info for debugging
                System.out.println("📁 Directory exists: " + directory.exists());
                System.out.println("📁 Directory is writable: " + directory.canWrite());
                System.out.println("📁 Directory absolute path: " + directory.getAbsolutePath());

                // Generate unique filename
                String filename = user.getId() + "_" + System.currentTimeMillis() + "_" +
                        StringUtils.cleanPath(profileImage.getOriginalFilename());
                String filePath = uploadDir + "/" + filename;
                System.out.println("📄 Saving profile image to: " + filePath);

                // Save the file using direct transfer to a ByteArrayOutputStream first
                byte[] bytes = profileImage.getBytes();
                Path path = Paths.get(filePath);
                Files.write(path, bytes);

                // Verify file was written
                File outputFile = new File(filePath);
                System.out.println("💾 File saved successfully: " + outputFile.exists() + " - Size: " + outputFile.length() + " bytes");

                // Update user profile image URL - use a URL path that will be accessible by the front-end
                profileImageUrl = baseUrl + "/uploads/profile-images/" + filename;
                String oldImageUrl = user.getProfileImageUrl();
                user.setProfileImageUrl(profileImageUrl);
                updated = true;
                System.out.println("🖼️ Updated profile image URL: '" + oldImageUrl + "' -> '" + profileImageUrl + "'");
            } catch (IOException e) {
                System.err.println("❌ Failed to upload profile image: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "Failed to upload profile image: " + e.getMessage()));
            }
        }

        // Save user if anything was updated
        if (updated) {
            try {
                User savedUser = userRepository.save(user);
                System.out.println("✅ User saved successfully. Updated fields: " +
                        "displayName=" + savedUser.getDisplayName() +
                        ", bio=" + savedUser.getBio() +
                        ", profileImageUrl=" + savedUser.getProfileImageUrl());

                // Verify the data was actually saved by making a fresh query
                Optional<User> verifyUser = userRepository.findById(user.getId());
                if (verifyUser.isPresent()) {
                    User freshUser = verifyUser.get();
                    System.out.println("🔍 Verification query: " +
                            "displayName=" + freshUser.getDisplayName() +
                            ", bio=" + freshUser.getBio() +
                            ", profileImageUrl=" + freshUser.getProfileImageUrl());
                } else {
                    System.out.println("⚠️ Verification query failed: User not found after save");
                }
            } catch (Exception e) {
                System.err.println("❌ Failed to save user: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "Failed to save user: " + e.getMessage()));
            }
        } else {
            System.out.println("ℹ️ No changes to save");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Profile updated successfully");

        // Include the user data in the response
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("username", user.getUsername());
        userData.put("email", user.getEmail());
        userData.put("displayName", user.getDisplayName());
        userData.put("bio", user.getBio());
        userData.put("profileImageUrl", user.getProfileImageUrl());

        response.put("user", userData);

        // Include profile image URL if it was updated
        if (profileImageUrl != null) {
            response.put("profileImageUrl", profileImageUrl);
        }

        System.out.println("✅ Profile update response: " + response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/users/profile-image-status")
    public ResponseEntity<?> checkProfileImageStatus(@RequestParam String url) {
        try {
            // Extract the file path from the URL
            String filePath = url.replace(baseUrl, "");
            File file = new File(filePath);

            if (file.exists() && file.canRead()) {
                return ResponseEntity.ok(Map.of(
                        "status", "ok",
                        "exists", true,
                        "size", file.length()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "status", "not_found",
                        "exists", false
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Check if a user's account is private
     */
    @GetMapping("/{userId}/privacy-status")
    public ResponseEntity<Map<String, Boolean>> checkPrivacyStatus(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check user privacy settings
        UserPrivacySettings privacySettings = user.getPrivacySettings();
        boolean isPrivate = privacySettings != null && !privacySettings.isPublicProfile();

        Map<String, Boolean> response = new HashMap<>();
        response.put("isPrivate", isPrivate);

        return ResponseEntity.ok(response);
    }

    /**
     * Check if current user has sent a follow request to the target user
     */
    @GetMapping("/follow/request-status/{userId}")
    public ResponseEntity<?> checkFollowRequestStatus(@PathVariable Long userId) {
        User currentUser = userService.getCurrentUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean hasRequest = followRequestService.hasPendingRequest(currentUser, targetUser);

        return ResponseEntity.ok(Map.of(
                "hasRequest", hasRequest
        ));
    }

    /**
     * Follow a user by ID - handles privacy settings appropriately
     */
    @PostMapping("/follow-by-id/{userId}")
    @PreAuthorize("isAuthenticated()") // Add this to ensure authentication
    public ResponseEntity<?> followUserById(@PathVariable Long userId) {
        System.out.println("⭐ Follow-by-id endpoint called for user ID: " + userId);

        try {
            // Explicitly get the current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getName().equals("anonymousUser")) {
                System.out.println("❌ No valid authentication in follow-by-id");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            String email = authentication.getName();
            Optional<User> currentUserOpt = userRepository.findByEmail(email);

            if (currentUserOpt.isEmpty()) {
                System.out.println("❌ Current user not found with email: " + email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Current user not found"));
            }

            User currentUser = currentUserOpt.get();
            System.out.println("✅ Current user found: " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ")");

            // Get target user
            Optional<User> targetUserOpt = userRepository.findById(userId);
            if (targetUserOpt.isEmpty()) {
                System.out.println("❌ Target user not found with ID: " + userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Target user not found"));
            }

            User targetUser = targetUserOpt.get();
            System.out.println("✅ Target user found: " + targetUser.getUsername() + " (ID: " + targetUser.getId() + ")");

            // Check if trying to follow self
            if (currentUser.getId().equals(targetUser.getId())) {
                System.out.println("❌ User trying to follow themselves");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Cannot follow yourself"));
            }

            // Check privacy settings directly
            boolean isPrivate = privacySettingsService.isAccountPrivate(userId);
            System.out.println("🔒 Target account privacy: " + (isPrivate ? "PRIVATE" : "PUBLIC"));

            // Use the followRequestService to create a follow or request
            boolean directFollow = followService.createFollowOrRequest(userId);
            System.out.println("📊 Result: " + (directFollow ? "Direct follow" : "Follow request"));

            // Get updated stats
            int followersCount = followService.getFollowerCount(userId);
            int followingCount = followService.getFollowingCount(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            if (directFollow) {
                response.put("message", "Successfully followed user");
                response.put("followStatus", "following");
                response.put("isFollowing", true);
                response.put("isRequested", false);
            } else {
                response.put("message", "Follow request sent");
                response.put("followStatus", "requested");
                response.put("isFollowing", false);
                response.put("isRequested", true);
            }
            response.put("followersCount", followersCount);
            response.put("followingCount", followingCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Error in followUserById: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

}
