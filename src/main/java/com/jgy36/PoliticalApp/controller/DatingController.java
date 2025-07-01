package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.entity.*;
import com.jgy36.PoliticalApp.service.DatingService;
import com.jgy36.PoliticalApp.service.UserService;
import com.jgy36.PoliticalApp.service.MockDataService;
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

    @PostMapping("/profile")
    public ResponseEntity<DatingProfile> createOrUpdateProfile(
            @RequestBody DatingProfile profileData,
            Authentication authentication) {

        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        DatingProfile profile = datingService.createOrUpdateDatingProfile(user, profileData);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/profile/me")
    public ResponseEntity<DatingProfile> getCurrentUserProfile(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        DatingProfile profile = datingService.getDatingProfileByUser(user);

        if (profile == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(profile);
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

    // ADD this temporary debug endpoint to your DatingController.java
    @GetMapping("/debug/stats")
    public ResponseEntity<?> getDebugStats() {
        try {
            long totalUsers = userService.getTotalUserCount(); // You'll need to create this method
            long datingProfiles = datingService.getTotalDatingProfileCount(); // You'll need to create this method
            long mockUsers = userService.getMockUserCount(); // Count users with "@mockdating.app" emails

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
    // ADD this simple debug method to your DatingController.java

    // Then add this debug method:
    @GetMapping("/debug/users")
    public ResponseEntity<?> debugUsers(Authentication authentication) {
        try {
            User currentUser = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get potential matches using existing method
            List<DatingProfile> potentialMatches = datingService.getPotentialMatches(currentUser);

            // Get all dating profiles to see what's in the database
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
                            .collect(Collectors.toList()) // Changed from .toList() to .collect(Collectors.toList())
            );

            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // ADD this method to your DatingController.java for debugging:

    @GetMapping("/debug/database")
    public ResponseEntity<?> debugDatabase(Authentication authentication) {
        try {
            User currentUser = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check what's in the database
            List<User> allUsers = userRepository.findAll();
            List<User> mockUsers = userRepository.findByEmailContaining("@mockdating.app");
            List<DatingProfile> allProfiles = datingProfileRepository.findAll();
            List<DatingProfile> activeProfiles = datingProfileRepository.findAll().stream()
                    .filter(p -> p.getIsActive())
                    .collect(Collectors.toList());

            // Test the actual query that's failing
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
    /**
     * Mark a match as seen (removes "new match" status)
     */
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
}
