package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.dto.DatingPreferencesRequest;
import com.jgy36.PoliticalApp.entity.DatingProfile;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.service.DatingService;
import com.jgy36.PoliticalApp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dating/settings")
public class DatingSettingsController {

    private static final Logger logger = LoggerFactory.getLogger(DatingSettingsController.class);

    @Autowired
    private DatingService datingService;

    @Autowired
    private UserService userService;

    @PostMapping("/confirm-age")
    public ResponseEntity<?> confirmAge() {
        try {
            User user = userService.getCurrentUser();
            logger.info("Confirming age for user: {}", user.getId());

            User updatedUser = userService.confirmAge(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "ageConfirmed", true,
                    "eligibleForDating", updatedUser.isEligibleForDating()
            ));
        } catch (Exception e) {
            logger.error("Error confirming age: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/preferences")
    public ResponseEntity<?> updateDatingPreferences(
            @RequestBody DatingPreferencesRequest request) {
        try {
            User user = userService.getCurrentUser();
            logger.info("Updating dating preferences for user: {}", user.getId());
            logger.info("Request data: {}", request.toString());

            // Validate the request
            if (request.getGenderPreference() == null) {
                logger.warn("Gender preference is null in request");
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Gender preference is required"
                ));
            }

            DatingProfile profile = datingService.updateDatingPreferences(user, request);
            logger.info("Successfully updated dating preferences for user: {}", user.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "preferences", profile
            ));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument in dating preferences: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Invalid preference value: " + e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error updating dating preferences: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/eligibility")
    public ResponseEntity<?> getDatingEligibility() {
        try {
            User user = userService.getCurrentUser();
            logger.info("Getting dating eligibility for user: {}", user.getId());

            return ResponseEntity.ok(Map.of(
                    "age", user.getAge(),
                    "ageConfirmed", user.getAgeConfirmed(),
                    "eligibleForDating", user.isEligibleForDating(),
                    "hasDatingProfile", datingService.getDatingProfileByUser(user) != null
            ));
        } catch (Exception e) {
            logger.error("Error getting dating eligibility: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
