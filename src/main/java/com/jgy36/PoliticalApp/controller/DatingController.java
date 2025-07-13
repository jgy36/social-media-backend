package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.annotation.RequireSubscription;
import com.jgy36.PoliticalApp.entity.*;
import com.jgy36.PoliticalApp.dto.DatingFilters;
import com.jgy36.PoliticalApp.repository.DatingProfileRepository;
import com.jgy36.PoliticalApp.repository.MatchRepository;
import com.jgy36.PoliticalApp.repository.SwipeRepository;
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

import java.time.LocalDateTime;
import java.util.*;
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

    @Autowired
    private SwipeRepository swipeRepository;

    @Autowired
    private MatchRepository matchRepository;

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
            @RequestParam(required = false) String education,
            @RequestParam(required = false) String lifestyle,
            @RequestParam(required = false) String religion,
            @RequestParam(required = false) String relationshipType,
            @RequestParam(required = false) String drinking,
            @RequestParam(required = false) String smoking,
            @RequestParam(required = false) String hasChildren,
            @RequestParam(required = false) String wantChildren,
            Authentication authentication) {

        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if advanced filters are being used
            boolean usingAdvancedFilters = education != null || lifestyle != null ||
                    religion != null || relationshipType != null ||
                    drinking != null || smoking != null ||
                    hasChildren != null || wantChildren != null;

            // Check subscription for advanced filters
            if (usingAdvancedFilters && !subscriptionService.canPerformAction("advanced_filters")) {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                        .body(Collections.emptyList());
            }

            // Check passport mode for location changes
            if (location != null && !location.isEmpty()) {
                DatingProfile userProfile = datingService.getDatingProfileByUser(user);
                String userLocation = userProfile != null ? userProfile.getLocation() : null;

                if (!location.equals(userLocation) && !subscriptionService.canPerformAction("passport_mode")) {
                    return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                            .body(Collections.emptyList());
                }
            }

            // Create filter object
            DatingFilters filters = new DatingFilters();
            filters.setLocation(location);
            filters.setEducation(education);
            filters.setLifestyle(lifestyle);
            filters.setReligion(religion);
            filters.setRelationshipType(relationshipType);
            filters.setDrinking(drinking);
            filters.setSmoking(smoking);
            filters.setHasChildren(hasChildren);
            filters.setWantChildren(wantChildren);

            List<DatingProfile> matches;
            if (useAlgorithm) {
                matches = datingService.getPotentialMatchesWithFilters(user, filters);
            } else {
                matches = datingService.getPotentialMatchesWithFilters(user, filters);
            }

            return ResponseEntity.ok(matches);

        } catch (Exception e) {
            logger.error("Error getting potential matches: ", e);
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
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

            // Check subscription limits for super likes
            if (direction == SwipeDirection.SUPER_LIKE && !subscriptionService.canPerformAction("super_like")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of(
                                "error", "You've reached your daily super like limit",
                                "upgradeRequired", true,
                                "errorCode", "SUPER_LIKE_LIMIT_EXCEEDED"
                        ));
            }

            Match match = datingService.swipeUser(swiper, target, direction);

            // Update Elo scores based on the swipe
            datingService.updateEloScores(swiper, target, direction);

            // Increment usage counters
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

            return ResponseEntity.ok(Map.of(
                    "matched", true,
                    "match", match,
                    "likeBack", true
            ));

        } catch (Exception e) {
            logger.error("Error during like back: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/undo-swipe")
    @RequireSubscription(tier = SubscriptionTier.ESSENTIAL, feature = "undo_swipe")
    public ResponseEntity<?> undoLastSwipe(Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<Swipe> lastSwipe = swipeRepository.findTopBySwiperOrderBySwipedAtDesc(user);

            if (lastSwipe.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "No swipes to undo"
                ));
            }

            Swipe swipeToUndo = lastSwipe.get();

            // Check if swipe is recent enough to undo (within last 30 minutes)
            LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
            if (swipeToUndo.getSwipedAt().isBefore(thirtyMinutesAgo)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Can only undo swipes from the last 30 minutes"
                ));
            }

            // If it was a match, remove the match too
            if (swipeToUndo.getDirection() == SwipeDirection.LIKE ||
                    swipeToUndo.getDirection() == SwipeDirection.SUPER_LIKE) {

                Optional<Swipe> reciprocalSwipe = swipeRepository.findBySwiperAndTarget(
                        swipeToUndo.getTarget(), user);

                if (reciprocalSwipe.isPresent() &&
                        (reciprocalSwipe.get().getDirection() == SwipeDirection.LIKE ||
                                reciprocalSwipe.get().getDirection() == SwipeDirection.SUPER_LIKE)) {

                    List<Match> matches = matchRepository.findActiveMatchesBetweenUsers(
                            user.getId(), swipeToUndo.getTarget().getId());
                    for (Match match : matches) {
                        match.setIsActive(false);
                        matchRepository.save(match);
                    }
                }
            }

            swipeRepository.delete(swipeToUndo);

            DatingProfile undoneProfile = datingService.getDatingProfileByUser(swipeToUndo.getTarget());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Swipe undone successfully",
                    "undoneProfile", undoneProfile,
                    "direction", swipeToUndo.getDirection().toString()
            ));

        } catch (Exception e) {
            logger.error("Error undoing swipe: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to undo swipe: " + e.getMessage()
            ));
        }
    }

    // ==================== PREMIUM FEATURES ====================

    @PostMapping("/boost")
    @RequireSubscription(tier = SubscriptionTier.ESSENTIAL, feature = "boost")
    public ResponseEntity<?> boostProfile(Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            DatingProfile profile = datingService.getDatingProfileByUser(user);
            if (profile == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "No dating profile found"
                ));
            }

            if (!subscriptionService.canPerformAction("boost")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                        "success", false,
                        "error", "You've reached your monthly boost limit",
                        "upgradeRequired", true
                ));
            }

            if (profile.getProfileBoostUntil() != null &&
                    profile.getProfileBoostUntil().isAfter(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Profile is already boosted",
                        "boostEndsAt", profile.getProfileBoostUntil()
                ));
            }

            LocalDateTime boostEnd = LocalDateTime.now().plusMinutes(30);
            profile.setProfileBoostUntil(boostEnd);
            datingProfileRepository.save(profile);

            subscriptionService.incrementUsage("boost");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile boosted for 30 minutes!",
                    "boostEndsAt", boostEnd,
                    "boostDurationMinutes", 30
            ));

        } catch (Exception e) {
            logger.error("Error boosting profile: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to boost profile: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/boost/status")
    public ResponseEntity<?> getBoostStatus(Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            DatingProfile profile = datingService.getDatingProfileByUser(user);
            if (profile == null) {
                return ResponseEntity.ok(Map.of(
                        "isBoosted", false,
                        "canBoost", false
                ));
            }

            boolean isBoosted = profile.getProfileBoostUntil() != null &&
                    profile.getProfileBoostUntil().isAfter(LocalDateTime.now());

            boolean canBoost = subscriptionService.canPerformAction("boost") && !isBoosted;

            Map<String, Object> response = new HashMap<>();
            response.put("isBoosted", isBoosted);
            response.put("canBoost", canBoost);

            if (isBoosted) {
                response.put("boostEndsAt", profile.getProfileBoostUntil());
                long minutesLeft = java.time.Duration.between(
                        LocalDateTime.now(), profile.getProfileBoostUntil()
                ).toMinutes();
                response.put("minutesLeft", minutesLeft);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting boost status: ", e);
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

        } catch (Exception e) {
            logger.error("Error getting who liked me: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== MATCHES ====================

    @GetMapping("/matches")
    public ResponseEntity<List<Match>> getUserMatches(Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Match> matches = datingService.getUserMatches(user);
            return ResponseEntity.ok(matches);
        } catch (Exception e) {
            logger.error("Error getting user matches: ", e);
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
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
            logger.error("Error marking match as seen: ", e);
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
            status.put("hasAdvancedFilters", subscription.getTier().ordinal() >= SubscriptionTier.PREMIUM.ordinal());

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

    // ==================== DEBUG ENDPOINTS ====================

    @GetMapping("/debug/stats")
    public ResponseEntity<?> getDebugStats() {
        try {
            long totalUsers = userService.getTotalUserCount();
            long datingProfiles = datingService.getTotalDatingProfileCount();
            long mockUsers = userService.getMockUserCount();

            return ResponseEntity.ok(Map.of(
                    "totalUsers", totalUsers,
                    "datingProfiles", datingProfiles,
                    "mockUsers", mockUsers
            ));
        } catch (Exception e) {
            logger.error("Error getting debug stats: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/generate-mock-users")
    public ResponseEntity<?> generateMockUsers(@RequestParam(defaultValue = "20") int count) {
        try {
            mockDataService.generateMockUsers(count);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Generated " + count + " mock users successfully"
            ));
        } catch (Exception e) {
            logger.error("Error generating mock users: ", e);
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
            logger.error("Error clearing mock data: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to clear mock data: " + e.getMessage()
            ));
        }
    }

    // ==================== HELPER METHODS ====================

    private DatingProfile convertMapToDatingProfile(Map<String, Object> data) {
        DatingProfile profile = new DatingProfile();

        // Basic fields
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

        // Vitals and preferences
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

        // Gender enum
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

        // Gender preference enum
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

        // Numeric fields
        if (data.get("minAge") != null) {
            profile.setMinAge(convertToInteger(data.get("minAge")));
        }
        if (data.get("maxAge") != null) {
            profile.setMaxAge(convertToInteger(data.get("maxAge")));
        }
        if (data.get("maxDistance") != null) {
            profile.setMaxDistance(convertToInteger(data.get("maxDistance")));
        }

        // Lists
        if (data.get("photos") != null) {
            @SuppressWarnings("unchecked")
            List<String> photos = (List<String>) data.get("photos");
            profile.setPhotos(photos);
        }
        if (data.get("prompts") != null) {
            @SuppressWarnings("unchecked")
            List<String> prompts = (List<String>) data.get("prompts");
            profile.setPrompts(prompts);
        }
        if (data.get("interests") != null) {
            @SuppressWarnings("unchecked")
            List<String> interests = (List<String>) data.get("interests");
            profile.setInterests(interests);
        }
        if (data.get("virtues") != null) {
            @SuppressWarnings("unchecked")
            List<String> virtues = (List<String>) data.get("virtues");
            profile.setVirtues(virtues);
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
}
