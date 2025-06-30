package com.jgy36.PoliticalApp.controller;

// PoliticalApp/src/main/java/com/jgy36/PoliticalApp/controller/FollowRequestController.java

import com.jgy36.PoliticalApp.entity.FollowRequest;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.service.FollowRequestService;
import com.jgy36.PoliticalApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/follow/requests")
@CrossOrigin(origins = "http://localhost:3000") // Add this line
public class FollowRequestController {

    @Autowired
    private FollowRequestService followRequestService;

    @Autowired
    private UserService userService;

    /**
     * Get all follow requests for the current user
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getFollowRequests() {
        User currentUser = userService.getCurrentUser();
        List<FollowRequest> requests = followRequestService.getFollowRequestsForUser(currentUser);

        List<Map<String, Object>> requestDTOs = requests.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(requestDTOs);
    }

    /**
     * Approve a follow request
     */
    @PostMapping("/{requestId}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> approveRequest(@PathVariable Long requestId) {
        followRequestService.approveFollowRequest(requestId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Follow request approved");

        return ResponseEntity.ok(response);
    }

    /**
     * Reject a follow request
     */
    @PostMapping("/{requestId}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> rejectRequest(@PathVariable Long requestId) {
        followRequestService.rejectFollowRequest(requestId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Follow request rejected");

        return ResponseEntity.ok(response);
    }

    /**
     * Convert a follow request to a DTO
     */
    private Map<String, Object> convertToDTO(FollowRequest request) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", request.getId());
        dto.put("userId", request.getRequester().getId());
        dto.put("username", request.getRequester().getUsername());
        dto.put("displayName", request.getRequester().getDisplayName());
        dto.put("profileImageUrl", request.getRequester().getProfileImageUrl());
        dto.put("requestedAt", request.getCreatedAt().toString());

        return dto;
    }
}
