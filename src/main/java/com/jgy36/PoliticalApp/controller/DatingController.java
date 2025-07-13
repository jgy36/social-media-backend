package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.annotation.RequireSubscription;
import com.jgy36.PoliticalApp.entity.*;
import com.jgy36.PoliticalApp.repository.DatingProfileRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import com.jgy36.PoliticalApp.service.DatingService;
import com.jgy36.PoliticalApp.service.MockDataService;
import com.jgy36.PoliticalApp.service.SubscriptionService;
import com.jgy36.PoliticalApp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.jgy36.PoliticalApp.entity.SubscriptionRequiredException;  // ADD THIS


import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dating")
@CrossOrigin(origins = "http://localhost:3000")
public class DatingController {

    private static final Logger logger = LoggerFactory.getLogger(DatingController.class);

    @Autowired
    private DatingService datingService;

    @Autowired
    private UserService userService;

    @Autowired
    private MockDataService mockDataService;

    @Autowired
    private DatingProfileRepository datingProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    // ==================== PROFILE MANAGEMENT ====================

    @PostMapping("/profile")
    public ResponseEntity<?> createOrUpdateProfile(
            @RequestBody Map<String, Object> profileData,
            Authentication authentication) {

        try {
            logger.info("Creating/updating dating profile");

            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            DatingProfile profile = convertMapToDatingProfile(profileData);
            DatingProfile savedProfile = datingService.createOrUpdateDatingProfile(user, profile);

            logger.info("Successfully saved profile with ID: {}", savedProfile.getId());
            return ResponseEntity.ok(savedProfile);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument in dating profile: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid profile data: " + e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error creating/updating profile: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to save profile: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/profile/me")
    public ResponseEntity<DatingProfile> getCurrentUserProfile(Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            DatingProfile profile = datingService.getDatingProfileByUser(user);
            if (profile == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            logger.error("Error getting current user profile: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/profile/user/{userId}")
    public ResponseEntity<DatingProfile> getUserDatingProfile(
            @PathVariable Long userId,
            Authentication authentication) {
        try {
            User currentUser = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            User targetUser = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            // Check if users are matched or it's their own profile
            boolean areMatched = datingService.areUsersMatched(currentUser, targetUser);
            if (!areMatched && !userId.equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            DatingProfile profile = datingService.getDatingProfileByUser(targetUser);
            if (profile == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            logger.error("Error getting user dating profile: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== DISCOVERY & MATCHING ====================

    @GetMapping("/potential-matches")
    public ResponseEntity<List<DatingProfile>> getPotentialMatches(
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "true") boolean useAlgorithm,
            Authentication authentication) {

        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (location != null && !location.isEmpty()) {
            if (!subscriptionService.canPerformAction("passport_mode")) {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                        .body(Collections.emptyList());
            }
        }

        List<DatingProfile> matches;
        if (useAlgorithm) {
            matches = datingService.getPotentialMatchesWithAlgorithm(user);
        } else {
            matches = datingService.getPotentialMatches(user); // Keep old method as fallback
        }

        return ResponseEntity.ok(matches);
    }


    // ==================== SWIPE ACTIONS ====================

    @PostMapping("/swipe")
    public ResponseEntity<?> swipeUser(
            @RequestParam Long targetUserId,
            @RequestParam SwipeDirection direction,
            Authentication authentication) {

        try {
            User swiper = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            User target = userService.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            if (direction == SwipeDirection.SUPER_LIKE) {
                if (!subscriptionService.canPerformAction("super_like")) {
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                            .body(Map.of(
                                    "error", "You've reached your daily super like limit",
                                    "upgradeRequired", true,
                                    "errorCode", "SUPER_LIKE_LIMIT_EXCEEDED"
                            ));
                }
            }

            Match match = datingService.swipeUser(swiper, target, direction);

            // Update Elo scores based on the swipe
            datingService.updateEloScores(swiper, target, direction);

            if (direction == SwipeDirection.SUPER_LIKE) {
                subscriptionService.incrementUsage("super_like");
            } else {
                subscriptionService.incrementUsage("swipe");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("matched", match != null);
            response.put("superLike", direction == SwipeDirection.SUPER_LIKE);
            if (match != null) {
                response.put("match", match);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during swipe: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/super-like")
    public ResponseEntity<?> superLikeUser(
            @RequestParam Long targetUserId,
            Authentication authentication) {

        try {
            User swiper = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            User target = userService.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            Match match = datingService.swipeUser(swiper, target, SwipeDirection.SUPER_LIKE);

            return ResponseEntity.ok(Map.of(
                    "matched", match != null,
                    "match", match,
                    "superLike", true
            ));

        } catch (Exception e) {
            logger.error("Error during super like: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/undo-swipe")
    public ResponseEntity<?> undoLastSwipe(Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // TODO: Implement undo logic in DatingService
            // boolean undoSuccessful = datingService.undoLastSwipe(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Last swipe undone"
            ));

        } catch (Exception e) {
            logger.error("Error undoing swipe: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== PREMIUM FEATURES ====================

    @PostMapping("/boost")
    public ResponseEntity<?> boostProfile(Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // TODO: Implement boost logic in DatingService
            // datingService.boostProfile(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile boosted for 30 minutes!",
                    "boostEndsAt", LocalDateTime.now().plusMinutes(30)
            ));

        } catch (Exception e) {
            logger.error("Error boosting profile: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/who-liked-me")
    @RequireSubscription(tier = SubscriptionTier.ESSENTIAL, feature = "see_likes")
    public ResponseEntity<?> getWhoLikedMe(Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<DatingProfile> likes = datingService.getWhoLikedMe(user);

            return ResponseEntity.ok(Map.of(
                    "likes", likes,
                    "count", likes.size(),
                    "tier", subscriptionService.getCurrentUserSubscription().getTier().getDisplayName()
            ));

        } catch (SubscriptionRequiredException e) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(Map.of(
                            "error", e.getMessage(),
                            "upgradeRequired", true,
                            "feature", "see_likes"
                    ));
        } catch (Exception e) {
            logger.error("Error getting who liked me: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== MATCHES ====================

    @GetMapping("/matches")
    public ResponseEntity<List<Match>> getUserMatches(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Match> matches = datingService.getUserMatches(user);
        return ResponseEntity.ok(matches);
    }

    @PostMapping("/matches/{matchId}/mark-seen")
    public ResponseEntity<?> markMatchAsSeen(
            @PathVariable Long matchId,
            Authentication authentication) {

        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            datingService.markMatchAsSeen(matchId, user);
            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/match-status/{userId}")
    public ResponseEntity<?> checkMatchStatus(
            @PathVariable Long userId,
            Authentication authentication) {
        try {
            User currentUser = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            User targetUser = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            boolean areMatched = datingService.areUsersMatched(currentUser, targetUser);
            return ResponseEntity.ok(Map.of("isMatched", areMatched));

        } catch (Exception e) {
            logger.error("Error checking match status: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== SUBSCRIPTION STATUS ====================

    @GetMapping("/subscription-status")
    public ResponseEntity<?> getSubscriptionStatus(Authentication authentication) {
        try {
            var subscription = subscriptionService.getCurrentUserSubscription();
            var subscriptionDTO = subscriptionService.toDTO(subscription);

            Map<String, Object> status = new HashMap<>();
            status.put("tier", subscription.getTier().getDisplayName());
            status.put("canSwipe", subscriptionService.canPerformAction("swipe"));
            status.put("canSuperLike", subscriptionService.canPerformAction("super_like"));
            status.put("canBoost", subscriptionService.canPerformAction("boost"));
            status.put("hasPassportMode", subscription.getTier().ordinal() >= SubscriptionTier.PREMIUM.ordinal());
            status.put("canSeeWhoLikedMe", subscription.getTier().ordinal() >= SubscriptionTier.PREMIUM.ordinal());
            status.put("canUndoSwipes", subscription.getTier().ordinal() >= SubscriptionTier.ESSENTIAL.ordinal());

            // Usage stats
            status.put("dailySwipesUsed", subscriptionDTO.getDailySwipesUsed());
            status.put("dailySwipeLimit", subscriptionDTO.getDailySwipeLimit());
            status.put("dailySuperLikesUsed", subscriptionDTO.getDailySuperLikesUsed());
            status.put("dailySuperLikeLimit", subscriptionDTO.getDailySuperLikeLimit());
            status.put("monthlyBoostsUsed", subscriptionDTO.getMonthlyBoostsUsed());
            status.put("monthlyBoostLimit", subscriptionDTO.getMonthlyBoostLimit());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("Error getting subscription status: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== MOCK DATA & DEBUG ====================

    @PostMapping("/generate-mock-users")
    public ResponseEntity<?> generateMockUsers(@RequestParam(defaultValue = "20") int count) {
        try {
            mockDataService.generateMockUsers(count);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Generated " + count + " mock users successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to generate mock users: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/clear-mock-data")
    public ResponseEntity<?> clearMockData() {
        try {
            mockDataService.clearMockData();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mock data cleared successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to clear mock data: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/debug/stats")
    public ResponseEntity<?> getDebugStats() {
        try {
            long totalUsers = userService.getTotalUserCount();
            long datingProfiles = datingService.getTotalDatingProfileCount();
            long mockUsers = userService.getMockUserCount();

            Map<String, Object> stats = Map.of(
                    "totalUsers", totalUsers,
                    "datingProfiles", datingProfiles,
                    "mockUsers", mockUsers
            );

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/debug/users")
    public ResponseEntity<?> debugUsers(Authentication authentication) {
        try {
            User currentUser = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<DatingProfile> potentialMatches = datingService.getPotentialMatches(currentUser);
            List<DatingProfile> allProfiles = datingProfileRepository.findAll();

            Map<String, Object> debug = Map.of(
                    "currentUserId", currentUser.getId(),
                    "currentUserEmail", currentUser.getEmail(),
                    "allDatingProfilesCount", allProfiles.size(),
                    "activeDatingProfilesCount", allProfiles.stream()
                            .mapToInt(p -> p.getIsActive() ? 1 : 0).sum(),
                    "potentialMatchesCount", potentialMatches.size(),
                    "sampleProfiles", allProfiles.stream()
                            .limit(5)
                            .map(p -> Map.of(
                                    "id", p.getId(),
                                    "username", p.getUser().getUsername(),
                                    "email", p.getUser().getEmail(),
                                    "age", p.getAge(),
                                    "isActive", p.getIsActive(),
                                    "isMockUser", p.getUser().getEmail().contains("@mockdating.app")
                            ))
                            .collect(Collectors.toList())
            );

            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/debug/database")
    public ResponseEntity<?> debugDatabase(Authentication authentication) {
        try {
            User currentUser = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<User> allUsers = userRepository.findAll();
            List<User> mockUsers = userRepository.findByEmailContaining("@mockdating.app");
            List<DatingProfile> allProfiles = datingProfileRepository.findAll();
            List<DatingProfile> activeProfiles = datingProfileRepository.findAll().stream()
                    .filter(DatingProfile::getIsActive)
                    .collect(Collectors.toList());

            List<DatingProfile> potentialMatches = datingProfileRepository
                    .findActiveDatingProfilesExcludingUser(currentUser.getId());

            Map<String, Object> debug = Map.of(
                    "currentUserId", currentUser.getId(),
                    "currentUserEmail", currentUser.getEmail(),
                    "totalUsers", allUsers.size(),
                    "mockUsers", mockUsers.size(),
                    "totalDatingProfiles", allProfiles.size(),
                    "activeDatingProfiles", activeProfiles.size(),
                    "potentialMatchesFromQuery", potentialMatches.size(),
                    "mockUserEmails", mockUsers.stream().limit(3).map(User::getEmail).collect(Collectors.toList()),
                    "sampleActiveProfiles", activeProfiles.stream().limit(3).map(p -> Map.of(
                            "id", p.getId(),
                            "userId", p.getUser().getId(),
                            "username", p.getUser().getUsername(),
                            "email", p.getUser().getEmail(),
                            "isActive", p.getIsActive()
                    )).collect(Collectors.toList())
            );

            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/debug/potential-matches")
    public ResponseEntity<?> debugPotentialMatches(Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            DatingProfile userProfile = datingService.getDatingProfileByUser(user);
            if (userProfile == null) {
                return ResponseEntity.ok(Map.of("error", "No dating profile found"));
            }

            List<DatingProfile> matches = datingService.getPotentialMatches(user);

            Map<String, Object> debug = Map.of(
                    "userProfile", Map.of(
                            "id", userProfile.getId(),
                            "genderPreference", userProfile.getGenderPreference(),
                            "minAge", userProfile.getMinAge(),
                            "maxAge", userProfile.getMaxAge()
                    ),
                    "eligibleForDating", user.isEligibleForDating(),
                    "ageConfirmed", user.getAgeConfirmed(),
                    "potentialMatchesCount", matches.size(),
                    "sampleMatches", matches.stream().limit(3).map(p -> Map.of(
                            "id", p.getId(),
                            "username", p.getUser().getUsername(),
                            "gender", p.getGender(),
                            "age", p.getAge()
                    )).collect(Collectors.toList())
            );

            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/debug-profile-data")
    public ResponseEntity<?> debugProfileData(@RequestBody Map<String, Object> profileData) {
        logger.info("=== DEBUG PROFILE DATA ===");
        logger.info("Raw data: {}", profileData);
        logger.info("Gender value: '{}' (class: {})",
                profileData.get("gender"),
                profileData.get("gender") != null ? profileData.get("gender").getClass() : "null");

        try {
            if (profileData.get("gender") != null) {
                String genderStr = (String) profileData.get("gender");
                Gender gender = Gender.fromString(genderStr);
                logger.info("✅ Gender conversion successful: '{}' -> {}", genderStr, gender);
            }
        } catch (Exception e) {
            logger.error("❌ Gender conversion failed: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of("debug", "complete"));
    }

    @GetMapping("/debug/user")
    public ResponseEntity<?> debugCurrentUser(Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> debug = Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "dateOfBirth", user.getDateOfBirth(),
                    "ageConfirmed", user.getAgeConfirmed(),
                    "calculatedAge", user.getAge(),
                    "eligibleForDating", user.isEligibleForDating(),
                    "datingModeEnabled", user.getDatingModeEnabled()
            );

            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            logger.error("Debug user failed: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== HELPER METHODS ====================

    private DatingProfile convertMapToDatingProfile(Map<String, Object> data) {
        DatingProfile profile = new DatingProfile();

        // Handle basic fields
        if (data.get("bio") != null) {
            profile.setBio((String) data.get("bio"));
        }
        if (data.get("age") != null) {
            profile.setAge(convertToInteger(data.get("age")));
        }
        if (data.get("location") != null) {
            profile.setLocation((String) data.get("location"));
        }
        if (data.get("height") != null) {
            profile.setHeight((String) data.get("height"));
        }
        if (data.get("job") != null) {
            profile.setJob((String) data.get("job"));
        }
        if (data.get("religion") != null) {
            profile.setReligion((String) data.get("religion"));
        }
        if (data.get("relationshipType") != null) {
            profile.setRelationshipType((String) data.get("relationshipType"));
        }
        if (data.get("lifestyle") != null) {
            profile.setLifestyle((String) data.get("lifestyle"));
        }

        // Handle vitals and vices
        if (data.get("hasChildren") != null) {
            profile.setHasChildren((String) data.get("hasChildren"));
        }
        if (data.get("wantChildren") != null) {
            profile.setWantChildren((String) data.get("wantChildren"));
        }
        if (data.get("drinking") != null) {
            profile.setDrinking((String) data.get("drinking"));
        }
        if (data.get("smoking") != null) {
            profile.setSmoking((String) data.get("smoking"));
        }
        if (data.get("drugs") != null) {
            profile.setDrugs((String) data.get("drugs"));
        }
        if (data.get("lookingFor") != null) {
            profile.setLookingFor((String) data.get("lookingFor"));
        }

        // Handle Gender enum
        if (data.get("gender") != null) {
            try {
                String genderStr = (String) data.get("gender");
                Gender gender = parseGender(genderStr);
                profile.setGender(gender);
            } catch (Exception e) {
                logger.error("Failed to parse gender: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid gender: " + data.get("gender"));
            }
        }

        // Handle GenderPreference enum (optional)
        if (data.get("genderPreference") != null) {
            String genderPrefStr = (String) data.get("genderPreference");
            if (genderPrefStr != null && !genderPrefStr.trim().isEmpty() &&
                    !genderPrefStr.equals("null") && !genderPrefStr.equals("undefined")) {
                try {
                    GenderPreference genderPref = parseGenderPreference(genderPrefStr);
                    profile.setGenderPreference(genderPref);
                } catch (Exception e) {
                    logger.warn("Failed to parse gender preference '{}', setting to null: {}", genderPrefStr, e.getMessage());
                    profile.setGenderPreference(null);
                }
            }
        }

        // Handle numeric fields
        if (data.get("minAge") != null) {
            profile.setMinAge(convertToInteger(data.get("minAge")));
        }
        if (data.get("maxAge") != null) {
            profile.setMaxAge(convertToInteger(data.get("maxAge")));
        }
        if (data.get("maxDistance") != null) {
            profile.setMaxDistance(convertToInteger(data.get("maxDistance")));
        }

        // Handle lists
        if (data.get("photos") != null) {
            profile.setPhotos((List<String>) data.get("photos"));
        }
        if (data.get("prompts") != null) {
            profile.setPrompts((List<String>) data.get("prompts"));
        }
        if (data.get("interests") != null) {
            profile.setInterests((List<String>) data.get("interests"));
        }
        if (data.get("virtues") != null) {
            profile.setVirtues((List<String>) data.get("virtues"));
        }

        return profile;
    }

    private Gender parseGender(String genderStr) {
        switch (genderStr.toUpperCase().trim()) {
            case "MAN":
                return Gender.MAN;
            case "WOMAN":
                return Gender.WOMAN;
            case "NON_BINARY":
                return Gender.NON_BINARY;
            case "OTHER":
                return Gender.OTHER;
            default:
                throw new IllegalArgumentException("Unknown gender: " + genderStr);
        }
    }

    private GenderPreference parseGenderPreference(String genderPrefStr) {
        switch (genderPrefStr.toUpperCase().trim()) {
            case "MEN":
                return GenderPreference.MEN;
            case "WOMEN":
                return GenderPreference.WOMEN;
            case "EVERYONE":
                return GenderPreference.EVERYONE;
            case "NON_BINARY":
                return GenderPreference.NON_BINARY;
            default:
                throw new IllegalArgumentException("Unknown gender preference: " + genderPrefStr);
        }
    }

    private Integer convertToInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof String) return Integer.parseInt((String) value);
        if (value instanceof Double) return ((Double) value).intValue();
        throw new IllegalArgumentException("Cannot convert " + value + " to Integer");
    }

    // Add this method to DatingController.java
    @PostMapping("/like-back")
    public ResponseEntity<?> likeUserBack(
            @RequestParam Long targetUserId,
            Authentication authentication) {

        try {
            User swiper = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            User target = userService.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            Match match = datingService.likeUserBack(swiper, target);

            Map<String, Object> response = new HashMap<>();
            response.put("matched", true); // Always true for like back
            response.put("match", match);
            response.put("likeBack", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during like back: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/debug/database-schema")
    public ResponseEntity<?> debugDatabaseSchema() {
        try {
            // Get a sample dating profile to see what fields exist
            List<DatingProfile> allProfiles = datingProfileRepository.findAll();

            Map<String, Object> debug = new HashMap<>();
            debug.put("totalProfiles", allProfiles.size());

            if (!allProfiles.isEmpty()) {
                DatingProfile sample = allProfiles.get(0);
                debug.put("sampleProfile", Map.of(
                        "id", sample.getId(),
                        "hasEloScore", sample.getEloScore() != null,
                        "eloScore", sample.getEloScore(),
                        "hasFreshProfile", sample.getIsFreshProfile() != null,
                        "isFreshProfile", sample.getIsFreshProfile(),
                        "totalLikesReceived", sample.getTotalLikesReceived(),
                        "totalSwipesReceived", sample.getTotalSwipesReceived()
                ));
            }

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "type", e.getClass().getSimpleName()
            ));
        }
    }
}
