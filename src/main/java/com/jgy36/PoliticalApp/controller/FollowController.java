package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.service.FollowService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/follow")
@CrossOrigin(origins = "http://localhost:3000") // FIX: Use specific origin
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    // ✅ Follow a user and get updated counts
    @PostMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> followUser(@PathVariable Long userId) {
        Map<String, Object> result = followService.followUser(userId);
        return ResponseEntity.ok(result);
    }

    // ✅ Unfollow a user and get updated counts
    @DeleteMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> unfollowUser(@PathVariable Long userId) {
        Map<String, Object> result = followService.unfollowUser(userId);
        return ResponseEntity.ok(result);
    }

    // ✅ Check if current user is following a target user
    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Object>> getFollowStatus(@PathVariable Long userId) {
        Map<String, Object> status = followService.getFollowStatus(userId);
        return ResponseEntity.ok(status);
    }

    // ✅ Get list of IDs that current user follows
    @GetMapping("/following")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Long>> getFollowingIds() {
        return ResponseEntity.ok(followService.getFollowingIds());
    }

    // ✅ Get detailed follower stats
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Integer>> getFollowStats(@PathVariable Long userId) {
        int followers = followService.getFollowerCount(userId);
        int following = followService.getFollowingCount(userId);
        int posts = followService.getPostCount(userId);

        return ResponseEntity.ok(Map.of(
                "followersCount", followers,
                "followingCount", following,
                "postCount", posts
        ));
    }

    // ✅ NEW: Get followers list
    @GetMapping("/followers/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getFollowers(@PathVariable Long userId) {
        List<Map<String, Object>> followers = followService.getFollowers(userId);
        return ResponseEntity.ok(followers);
    }

    // ✅ NEW: Get following list
    @GetMapping("/following/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getFollowing(@PathVariable Long userId) {
        List<Map<String, Object>> following = followService.getFollowing(userId);
        return ResponseEntity.ok(following);
    }
}
