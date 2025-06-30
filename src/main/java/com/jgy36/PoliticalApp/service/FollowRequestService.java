package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.entity.Follow;
import com.jgy36.PoliticalApp.entity.FollowRequest;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.entity.UserPrivacySettings;
import com.jgy36.PoliticalApp.exception.ResourceNotFoundException;
import com.jgy36.PoliticalApp.repository.FollowRepository;
import com.jgy36.PoliticalApp.repository.FollowRequestRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FollowRequestService {

    @Autowired
    private FollowRequestRepository followRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PrivacySettingsService privacySettingsService;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Create a follow request or direct follow based on target user's privacy settings
     *
     * @param targetUserId ID of the user to follow
     * @return True if direct follow, false if request created
     */
    @Transactional
    public boolean createFollowOrRequest(Long targetUserId) {
        // Get current user using authentication context and email (like in UserController)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Current user not found by email: " + email));

        // Get target user
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Can't follow yourself
        if (currentUser.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }

        // Check if already following
        if (currentUser.getFollowing().contains(targetUser)) {
            return true; // Already following
        }

        // Check if already requested
        Optional<FollowRequest> existingRequest = followRequestRepository
                .findByRequesterAndTargetAndStatus(
                        currentUser,
                        targetUser,
                        FollowRequest.RequestStatus.PENDING
                );

        if (existingRequest.isPresent()) {
            return false; // Request already exists
        }

        // Get privacy settings directly from the repository instead of user object
        // This ensures we're getting the latest settings from the database
        UserPrivacySettings privacySettings = privacySettingsService
                .getSettings(targetUserId);

        boolean isPrivate = !privacySettings.isPublicProfile();

        System.out.println("CRITICAL DEBUG - Privacy check for user " + targetUser.getUsername());
        System.out.println("Is private account? " + isPrivate);
        System.out.println("Privacy settings: publicProfile=" + privacySettings.isPublicProfile());

        // If account is public, directly follow
        if (!isPrivate) {
            System.out.println("Creating direct follow - public account");
            currentUser.follow(targetUser);
            userRepository.save(currentUser);

            // Create follow notification for public account
            notificationService.createNotification(
                    targetUser,
                    currentUser.getUsername() + " started following you",
                    "follow",           // notification type
                    currentUser.getId(), // reference ID (follower ID)
                    null,               // no secondary reference
                    null                // no community context
            );

            return true;
        }

        // If account is private, create follow request
        System.out.println("Creating follow request - private account");
        FollowRequest followRequest = new FollowRequest(currentUser, targetUser);
        followRequestRepository.save(followRequest);

        // Create follow request notification for private account
        notificationService.createNotification(
                targetUser,
                currentUser.getUsername() + " requested to follow you",
                "follow_request",      // notification type
                currentUser.getId(),   // reference ID (requester ID)
                followRequest.getId(), // secondary reference (request ID)
                null                   // no community context
        );

        return false;
    }

    /**
     * Approve a follow request
     *
     * @param requestId ID of the request to approve
     */
    @Transactional
    public void approveFollowRequest(Long requestId) {
        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Current user not found by email: " + email));

        // Get the request
        FollowRequest request = followRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow request not found"));

        // Check if the request is for the current user
        if (!request.getTarget().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Cannot approve someone else's follow request");
        }

        // Check if request is pending
        if (request.getStatus() != FollowRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }

        // Approve the request
        request.setStatus(FollowRequest.RequestStatus.APPROVED);
        followRequestRepository.save(request);

        // Create the follow relationship
        User requester = request.getRequester();
        Follow follow = new Follow(requester, currentUser);
        followRepository.save(follow);

        // Create notification for request approval
        notificationService.createNotification(
                requester,
                currentUser.getUsername() + " accepted your follow request",
                "follow_request_approved", // notification type
                currentUser.getId(),       // reference ID (target user ID)
                null,                      // no secondary reference
                null                       // no community context
        );
    }

    /**
     * Reject a follow request
     *
     * @param requestId ID of the request to reject
     */
    @Transactional
    public void rejectFollowRequest(Long requestId) {
        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Current user not found by email: " + email));

        // Get the request
        FollowRequest request = followRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow request not found"));

        // Check if the request is for the current user
        if (!request.getTarget().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Cannot reject someone else's follow request");
        }

        // Check if request is pending
        if (request.getStatus() != FollowRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }

        // Reject the request
        request.setStatus(FollowRequest.RequestStatus.REJECTED);
        followRequestRepository.save(request);

        // Optional: Notify requester that their request was rejected
        notificationService.createNotification(
                request.getRequester(),
                currentUser.getUsername() + " declined your follow request",
                "follow_request_rejected", // notification type
                currentUser.getId(),       // reference ID (target user ID)
                null,                      // no secondary reference
                null                       // no community context
        );
    }

    /**
     * Get all follow requests for a user
     *
     * @param user User to get requests for
     * @return List of follow requests
     */
    public List<FollowRequest> getFollowRequestsForUser(User user) {
        return followRequestRepository.findByTargetAndStatus(user, FollowRequest.RequestStatus.PENDING);
    }

    // Other methods remain unchanged
    // ...

    /**
     * Check if user has a pending follow request to the target
     *
     * @param requester User who might have sent a request
     * @param target    Target user who might have received a request
     * @return True if there is a pending request
     */
    public boolean hasPendingRequest(User requester, User target) {
        return followRequestRepository
                .findByRequesterAndTargetAndStatus(
                        requester,
                        target,
                        FollowRequest.RequestStatus.PENDING
                )
                .isPresent();
    }
}
