package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.entity.*;
import com.jgy36.PoliticalApp.service.DatingService;
import com.jgy36.PoliticalApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dating")
@CrossOrigin(origins = "http://localhost:3000")
public class DatingController {

    @Autowired
    private DatingService datingService;

    @Autowired
    private UserService userService;

    @PostMapping("/profile")
    public ResponseEntity<DatingProfile> createOrUpdateProfile(
            @RequestBody DatingProfile profileData,
            Authentication authentication) {

        // Changed from findByUsername to findByEmail since JWT contains email
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        DatingProfile profile = datingService.createOrUpdateDatingProfile(user, profileData);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/profile/me")
    public ResponseEntity<DatingProfile> getCurrentUserProfile(Authentication authentication) {
        // Changed from findByUsername to findByEmail since JWT contains email
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
        // Changed from findByUsername to findByEmail since JWT contains email
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

        // Changed from findByUsername to findByEmail since JWT contains email
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
        // Changed from findByUsername to findByEmail since JWT contains email
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Match> matches = datingService.getUserMatches(user);
        return ResponseEntity.ok(matches);
    }
}
