package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.service.CommunityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CommunityService communityService;


    public AdminController(CommunityService communityService) {
        this.communityService = communityService;
    }

    /**
     * ✅ Test admin route (Only accessible to admins)
     */
    @GetMapping("/test")
    public ResponseEntity<String> testAdminAccess() {
        return ResponseEntity.ok("✅ You are an admin!");
    }

    /**
     * ✅ Initialize default communities
     */
    @PostMapping("/init-communities")
    public ResponseEntity<String> initCommunities() {
        try {
            communityService.forceInitDefaultCommunities();
            return ResponseEntity.ok("✅ Communities initialization completed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error initializing communities: " + e.getMessage());
        }
    }
}
