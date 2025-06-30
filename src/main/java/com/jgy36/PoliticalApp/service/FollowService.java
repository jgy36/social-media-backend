package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.entity.Follow;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.repository.FollowRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FollowRequestService followRequestService;

    public FollowService(FollowRepository followRepository, UserRepository userRepository, NotificationService notificationService, FollowRequestService followRequestService) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.followRequestService = followRequestService;
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional
    public Map<String, Object> followUser(Long userId) {
        User currentUser = getAuthenticatedUser();
        User userToFollow = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User to follow not found"));

        if (currentUser.equals(userToFollow)) {
            throw new IllegalArgumentException("You cannot follow yourself.");
        }

        boolean wasAlreadyFollowing = followRepository.existsByFollowerAndFollowing(currentUser, userToFollow);

        if (!wasAlreadyFollowing) {
            Follow follow = new Follow(currentUser, userToFollow);
            followRepository.save(follow);

            // Create notification for the followed user with updated format
            notificationService.createNotification(
                    userToFollow,
                    currentUser.getUsername() + " started following you",
                    "follow",           // notification type
                    currentUser.getId(), // reference ID (follower ID)
                    null,               // no secondary reference
                    null                // no community context
            );
        }

        // Always return updated follow counts
        int followersCount = getFollowerCount(userId);
        int followingCount = getFollowingCount(userId);

        // Explicitly specify the return type for Map.of to fix type inference error
        return Map.<String, Object>of(
                "success", true,
                "isFollowing", true,
                "followersCount", followersCount,
                "followingCount", followingCount,
                "message", wasAlreadyFollowing ? "Already following this user" : "User followed successfully"
        );
    }

    // Rest of the methods remain unchanged
    // ...


    @Transactional
    public Map<String, Object> unfollowUser(Long userId) {
        User currentUser = getAuthenticatedUser();
        User userToUnfollow = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User to unfollow not found"));

        boolean wasFollowing = followRepository.existsByFollowerAndFollowing(currentUser, userToUnfollow);

        if (wasFollowing) {
            followRepository.deleteByFollowerAndFollowing(currentUser, userToUnfollow);
        }

        // Always return updated follow counts
        int followersCount = getFollowerCount(userId);
        int followingCount = getFollowingCount(userId);

        // Explicitly specify the return type for Map.of to fix type inference error
        return Map.<String, Object>of(
                "success", true,
                "isFollowing", false,
                "followersCount", followersCount,
                "followingCount", followingCount,
                "message", wasFollowing ? "User unfollowed successfully" : "You were not following this user"
        );
    }

    public boolean checkIfFollowing(Long targetUserId) {
        User currentUser = getAuthenticatedUser();
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        return followRepository.existsByFollowerAndFollowing(currentUser, targetUser);
    }

    public List<Long> getFollowingIds() {
        User currentUser = getAuthenticatedUser();
        List<Follow> followings = followRepository.findByFollower(currentUser);
        return followings.stream()
                .map(follow -> follow.getFollowing().getId())
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getFollowers(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get current user to check if they follow each follower
        User currentUser = null;
        try {
            currentUser = getAuthenticatedUser();
        } catch (Exception e) {
            // User not authenticated, continue without this info
        }

        final User finalCurrentUser = currentUser;

        return followRepository.findFollowersByFollowingId(userId).stream()
                .map(follower -> {
                    boolean isFollowing = finalCurrentUser != null &&
                            followRepository.existsByFollowerAndFollowing(finalCurrentUser, follower);

                    return Map.<String, Object>of(
                            "id", follower.getId(),
                            "username", follower.getUsername(),
                            "isFollowing", isFollowing
                    );
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getFollowing(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get current user to check if they follow each following
        User currentUser = null;
        try {
            currentUser = getAuthenticatedUser();
        } catch (Exception e) {
            // User not authenticated, continue without this info
        }

        final User finalCurrentUser = currentUser;

        return followRepository.findFollowingByFollowerId(userId).stream()
                .map(following -> {
                    boolean isFollowing = finalCurrentUser != null &&
                            followRepository.existsByFollowerAndFollowing(finalCurrentUser, following);

                    return Map.<String, Object>of(
                            "id", following.getId(),
                            "username", following.getUsername(),
                            "isFollowing", isFollowing
                    );
                })
                .collect(Collectors.toList());
    }

    public int getFollowerCount(Long userId) {
        return userRepository.countFollowers(userId);
    }

    public int getFollowingCount(Long userId) {
        return userRepository.countFollowing(userId);
    }

    public int getPostCount(Long userId) {
        return userRepository.countPosts(userId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFollowStatus(Long targetUserId) {
        try {
            User currentUser = getAuthenticatedUser();
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

            boolean isFollowing = followRepository.existsByFollowerAndFollowing(currentUser, targetUser);
            int followersCount = getFollowerCount(targetUserId);
            int followingCount = getFollowingCount(targetUserId);

            // Explicitly specify the return type for Map.of to fix type inference error
            return Map.<String, Object>of(
                    "isFollowing", isFollowing,
                    "followersCount", followersCount,
                    "followingCount", followingCount
            );
        } catch (Exception e) {
            // Explicitly specify the return type for Map.of to fix type inference error
            return Map.<String, Object>of(
                    "isFollowing", false,
                    "followersCount", getFollowerCount(targetUserId),
                    "followingCount", getFollowingCount(targetUserId)
            );
        }
    }

    /**
     * Follow a user or create a follow request based on privacy settings
     *
     * @param targetUserId ID of the user to follow
     * @return true if direct follow created, false if request sent
     */
    @Transactional
    public boolean createFollowOrRequest(Long targetUserId) {
        // Delegate to the FollowRequestService
        return followRequestService.createFollowOrRequest(targetUserId);
    }
}
