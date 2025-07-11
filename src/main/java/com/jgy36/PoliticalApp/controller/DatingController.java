package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.entity.*;
import com.jgy36.PoliticalApp.service.DatingService;
import com.jgy36.PoliticalApp.service.UserService;
import com.jgy36.PoliticalApp.service.MockDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.jgy36.PoliticalApp.repository.DatingProfileRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;

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

    @PostMapping("/debug-profile-data")
    public ResponseEntity<?> debugProfileData(@RequestBody Map<String, Object> profileData) {
        logger.info("=== DEBUG PROFILE DATA ===");
        logger.info("Raw data: {}", profileData);
        logger.info("Gender value: '{}' (class: {})",
                profileData.get("gender"),
                profileData.get("gender") != null ? profileData.get("gender").getClass() : "null");

        // Test the gender conversion
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

            logger.info("=== USER DEBUG ===");
            logger.info("User ID: {}", user.getId());
            logger.info("User email: {}", user.getEmail());
            logger.info("User dateOfBirth (raw): {}", user.getDateOfBirth());
            logger.info("User ageConfirmed: {}", user.getAgeConfirmed());
            logger.info("User calculated age: {}", user.getAge());
            logger.info("User eligibleForDating: {}", user.isEligibleForDating());

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

    @PostMapping("/profile")
    public ResponseEntity<?> createOrUpdateProfile(
            @RequestBody Map<String, Object> profileData,
            Authentication authentication) {

        try {
            logger.info("=== CREATING/UPDATING DATING PROFILE ===");
            logger.info("Raw profile data received: {}", profileData);

            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            logger.info("User: {} (ID: {})", user.getEmail(), user.getId());

            // Convert the Map to DatingProfile entity safely
            DatingProfile profile = convertMapToDatingProfile(profileData);

            logger.info("Converted profile - Gender: {}, GenderPreference: {}",
                    profile.getGender(), profile.getGenderPreference());

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

    private DatingProfile convertMapToDatingProfile(Map<String, Object> data) {
        DatingProfile profile = new DatingProfile();

        logger.info("=== CONVERTING PROFILE DATA ===");
        logger.info("Keys in data: {}", data.keySet());
        logger.info("Gender value: '{}' (type: {})", data.get("gender"),
                data.get("gender") != null ? data.get("gender").getClass().getSimpleName() : "null");
        logger.info("Age value: '{}' (type: {})", data.get("age"),
                data.get("age") != null ? data.get("age").getClass().getSimpleName() : "null");

        // Handle basic fields
        if (data.get("bio") != null) {
            profile.setBio((String) data.get("bio"));
            logger.info("✅ Set bio: {}", ((String) data.get("bio")).substring(0, Math.min(50, ((String) data.get("bio")).length())));
        }

        // Handle age with detailed logging
        if (data.get("age") != null) {
            try {
                Integer age = convertToInteger(data.get("age"));
                profile.setAge(age);
                logger.info("✅ Set age: {}", age);
            } catch (Exception e) {
                logger.error("❌ Failed to convert age '{}': {}", data.get("age"), e.getMessage());
            }
        } else {
            logger.warn("⚠️ Age is null in request data");
        }

        if (data.get("location") != null) {
            profile.setLocation((String) data.get("location"));
            logger.info("✅ Set location: {}", profile.getLocation());
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

        // Handle Gender enum with manual conversion
        if (data.get("gender") != null) {
            try {
                String genderStr = (String) data.get("gender");
                logger.info("🔄 Processing gender string: '{}'", genderStr);

                // Manual conversion to avoid enum issues
                Gender gender = null;
                switch (genderStr.toUpperCase().trim()) {
                    case "MAN":
                        gender = Gender.MAN;
                        break;
                    case "WOMAN":
                        gender = Gender.WOMAN;
                        break;
                    case "NON_BINARY":
                        gender = Gender.NON_BINARY;
                        break;
                    case "OTHER":
                        gender = Gender.OTHER;
                        break;
                    default:
                        logger.error("Unknown gender value: '{}'", genderStr);
                        throw new IllegalArgumentException("Unknown gender: " + genderStr);
                }

                profile.setGender(gender);
                logger.info("✅ Successfully set gender: '{}' -> {}", genderStr, gender);

            } catch (Exception e) {
                logger.error("❌ Failed to parse gender '{}': {}", data.get("gender"), e.getMessage());
                throw new IllegalArgumentException("Invalid gender: " + data.get("gender"));
            }
        } else {
            logger.warn("⚠️ Gender is null in request data");
        }

        // Handle GenderPreference enum safely - MAKE IT OPTIONAL
        if (data.get("genderPreference") != null) {
            String genderPrefStr = (String) data.get("genderPreference");
            logger.info("🔄 Processing gender preference: '{}'", genderPrefStr);

            // Skip if it's empty, null, or placeholder values
            if (genderPrefStr != null && !genderPrefStr.trim().isEmpty() &&
                    !genderPrefStr.equals("null") && !genderPrefStr.equals("undefined")) {
                try {
                    // Manual conversion for gender preference too
                    GenderPreference genderPref = null;
                    switch (genderPrefStr.toUpperCase().trim()) {
                        case "MEN":
                            genderPref = GenderPreference.MEN;
                            break;
                        case "WOMEN":
                            genderPref = GenderPreference.WOMEN;
                            break;
                        case "EVERYONE":
                            genderPref = GenderPreference.EVERYONE;
                            break;
                        case "NON_BINARY":
                            genderPref = GenderPreference.NON_BINARY;
                            break;
                        default:
                            logger.error("Unknown gender preference value: '{}'", genderPrefStr);
                            throw new IllegalArgumentException("Unknown gender preference: " + genderPrefStr);
                    }

                    profile.setGenderPreference(genderPref);
                    logger.info("✅ Successfully set gender preference: '{}' -> {}", genderPrefStr, genderPref);
                } catch (Exception e) {
                    logger.warn("Failed to parse gender preference '{}', setting to null: {}", genderPrefStr, e.getMessage());
                    // Don't throw error, just leave it null - user can set it later in settings
                    profile.setGenderPreference(null);
                }
            } else {
                logger.info("Gender preference is empty/null, leaving unset");
                profile.setGenderPreference(null);
            }
        } else {
            logger.info("No gender preference provided, leaving unset");
            profile.setGenderPreference(null);
        }

        // Handle numeric fields safely
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
            logger.info("✅ Set {} photos", profile.getPhotos().size());
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

        logger.info("=== FINAL PROFILE ===");
        logger.info("Final gender: {}", profile.getGender());
        logger.info("Final age: {}", profile.getAge());
        logger.info("Final gender preference: {}", profile.getGenderPreference());

        return profile;
    }

    private Integer convertToInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof String) return Integer.parseInt((String) value);
        if (value instanceof Double) return ((Double) value).intValue();
        throw new IllegalArgumentException("Cannot convert " + value + " to Integer");
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

    @GetMapping("/potential-matches")
    public ResponseEntity<List<DatingProfile>> getPotentialMatches(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<DatingProfile> matches = datingService.getPotentialMatches(user);
        return ResponseEntity.ok(matches);
    }

    @PostMapping("/swipe")
    public ResponseEntity<?> swipeUser(
            @RequestParam Long targetUserId,
            @RequestParam SwipeDirection direction,
            Authentication authentication) {

        User swiper = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        User target = userService.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        Match match = datingService.swipeUser(swiper, target, direction);

        if (match != null) {
            return ResponseEntity.ok(Map.of("matched", true, "match", match));
        } else {
            return ResponseEntity.ok(Map.of("matched", false));
        }
    }

    @GetMapping("/matches")
    public ResponseEntity<List<Match>> getUserMatches(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Match> matches = datingService.getUserMatches(user);
        return ResponseEntity.ok(matches);
    }

    // MOCK DATA ENDPOINTS FOR TESTING
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
                    .filter(p -> p.getIsActive())
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

    /**
     * Get another user's dating profile (only if matched or public)
     */
    @GetMapping("/profile/user/{userId}")
    public ResponseEntity<DatingProfile> getUserDatingProfile(
            @PathVariable Long userId,
            Authentication authentication) {
        try {
            User currentUser = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            User targetUser = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            // Check if users are matched (implement this check)
            boolean areMatched = datingService.areUsersMatched(currentUser, targetUser);

            if (!areMatched && !userId.equals(currentUser.getId())) {
                return ResponseEntity.status(403).build(); // Forbidden
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

    /**
     * Check if current user is matched with another user
     */
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
}
