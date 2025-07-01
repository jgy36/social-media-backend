package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.entity.Match;
import com.jgy36.PoliticalApp.entity.Swipe;
import com.jgy36.PoliticalApp.entity.SwipeDirection;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.repository.MatchRepository;
import com.jgy36.PoliticalApp.repository.SwipeRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import com.jgy36.PoliticalApp.service.MockDataService;
import com.jgy36.PoliticalApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// ADD these methods to your DebugController.java (or create it if you don't have one)

@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "http://localhost:3000")
public class DebugController {

    @Autowired
    private MockDataService mockDataService;

    @Autowired
    private UserService userService;

    @Autowired
    private SwipeRepository swipeRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create test match scenarios for the current user
     */
    @PostMapping("/create-test-matches")
    public ResponseEntity<?> createTestMatches(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            mockDataService.createComprehensiveTestScenario(userEmail);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Test match scenarios created! Now swipe right on the first few profiles for instant matches!"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Check who has already liked you (for debugging)
     */
    @GetMapping("/who-likes-me")
    public ResponseEntity<?> whoLikesMe(Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Swipe> likesReceived = swipeRepository.findAll().stream()
                    .filter(swipe -> swipe.getTarget().getId().equals(user.getId()) &&
                            swipe.getDirection() == SwipeDirection.LIKE)
                    .collect(Collectors.toList());

            List<Map<String, Object>> likers = likesReceived.stream()
                    .map(swipe -> {
                        User liker = swipe.getSwiper();
                        Map<String, Object> likerInfo = new HashMap<>();
                        likerInfo.put("id", liker.getId());
                        likerInfo.put("username", liker.getUsername());
                        likerInfo.put("displayName", liker.getDisplayName());
                        likerInfo.put("swipedAt", swipe.getSwipedAt());
                        return likerInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "likers", likers,
                    "count", likers.size(),
                    "message", likers.size() + " users have liked you!"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Check your swipe history
     */
    @GetMapping("/my-swipes")
    public ResponseEntity<?> mySwipes(Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Swipe> mySwipes = swipeRepository.findAll().stream()
                    .filter(swipe -> swipe.getSwiper().getId().equals(user.getId()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> swipeHistory = mySwipes.stream()
                    .map(swipe -> {
                        User target = swipe.getTarget();
                        Map<String, Object> swipeInfo = new HashMap<>();
                        swipeInfo.put("targetId", target.getId());
                        swipeInfo.put("targetUsername", target.getUsername());
                        swipeInfo.put("direction", swipe.getDirection().toString());
                        swipeInfo.put("swipedAt", swipe.getSwipedAt());
                        return swipeInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "swipes", swipeHistory,
                    "totalSwipes", swipeHistory.size(),
                    "likes", swipeHistory.stream().filter(s -> "LIKE".equals(s.get("direction"))).count(),
                    "passes", swipeHistory.stream().filter(s -> "PASS".equals(s.get("direction"))).count()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Check your current matches
     */
    @GetMapping("/my-matches")
    public ResponseEntity<?> myMatches(Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Match> matches = matchRepository.findActiveMatchesForUser(user);

            List<Map<String, Object>> matchList = matches.stream()
                    .map(match -> {
                        User otherUser = match.getUser1().getId().equals(user.getId()) ?
                                match.getUser2() : match.getUser1();
                        Map<String, Object> matchInfo = new HashMap<>();
                        matchInfo.put("matchId", match.getId());
                        matchInfo.put("otherUserId", otherUser.getId());
                        matchInfo.put("otherUsername", otherUser.getUsername());
                        matchInfo.put("matchedAt", match.getMatchedAt());
                        return matchInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "matches", matchList,
                    "count", matchList.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Force create instant matches for testing (bypasses swipe system)
     */
    @PostMapping("/force-create-matches")
    public ResponseEntity<?> forceCreateMatches(Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get first 3 mock users
            List<User> mockUsers = userRepository.findByEmailContaining("@mockdating.app")
                    .stream()
                    .limit(3)
                    .collect(Collectors.toList());

            List<Match> createdMatches = new ArrayList<>();

            for (User mockUser : mockUsers) {
                // Check if match already exists
                if (matchRepository.findActiveMatchBetweenUsers(user, mockUser).isPresent()) {
                    continue;
                }

                // Create the match directly
                Match match = new Match();
                match.setUser1(user);
                match.setUser2(mockUser);
                match.setMatchedAt(LocalDateTime.now());
                match.setIsActive(true);

                Match savedMatch = matchRepository.save(match);
                createdMatches.add(savedMatch);

                System.out.println("✅ Force created match: " + user.getUsername() + " ↔ " + mockUser.getUsername());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Created " + createdMatches.size() + " instant matches!",
                    "matches", createdMatches.stream().map(m -> Map.of(
                            "matchId", m.getId(),
                            "otherUser", m.getUser2().getUsername(),
                            "matchedAt", m.getMatchedAt()
                    )).collect(Collectors.toList())
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Clear everything and start fresh
     */
    @PostMapping("/reset-everything")
    public ResponseEntity<?> resetEverything(Authentication authentication) {
        try {
            String userEmail = authentication.getName();

            // Clear existing data
            mockDataService.clearTestSwipes(userEmail);

            // Create fresh test scenario
            mockDataService.createComprehensiveTestScenario(userEmail);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Everything reset! 3 mock users now like you. Swipe right on them for instant matches!"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
