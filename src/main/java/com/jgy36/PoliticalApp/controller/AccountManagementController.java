package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.dto.ChangeEmailRequest;
import com.jgy36.PoliticalApp.dto.SocialConnectRequest;
import com.jgy36.PoliticalApp.dto.VerifyEmailRequest;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.repository.UserRepository;
import com.jgy36.PoliticalApp.service.AccountManagementService;
import jakarta.mail.MessagingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class AccountManagementController {
    private final AccountManagementService accountService;
    private final UserRepository userRepository;

    public AccountManagementController(AccountManagementService accountService, UserRepository userRepository) {
        this.accountService = accountService;
        this.userRepository = userRepository;
    }

    /**
     * Get email verification status
     */
    @GetMapping("/email/verification-status")
    public ResponseEntity<?> getEmailVerificationStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        boolean isVerified = accountService.isEmailVerified(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("isVerified", isVerified);
        response.put("email", email);

        return ResponseEntity.ok(response);
    }

    /**
     * Send verification email
     */
    @PostMapping("/email/send-verification")
    public ResponseEntity<?> sendVerificationEmail() {
        try {
            accountService.sendVerificationEmailToCurrentUser();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Verification email sent successfully"
            ));
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to send verification email: " + e.getMessage()
            ));
        }
    }

    /**
     * Verify email with code
     */
    @PostMapping("/email/verify")
    public ResponseEntity<?> verifyEmail(@RequestBody VerifyEmailRequest request) {
        boolean success = accountService.verifyCurrentUserEmail(request.getCode());

        if (!success) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid or expired verification code"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Email verified successfully"
        ));
    }

    /**
     * Request email change
     */
    @PostMapping("/email/change")
    public ResponseEntity<?> requestEmailChange(@RequestBody ChangeEmailRequest request) {
        try {
            accountService.requestCurrentUserEmailChange(request.getNewEmail());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Verification email sent to your new email address"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to send verification email: " + e.getMessage()
            ));
        }
    }

    /**
     * Get connected accounts
     */
    @GetMapping("/connected-accounts")
    public ResponseEntity<?> getConnectedAccounts() {
        Map<String, Boolean> accounts = accountService.getCurrentUserConnectedAccounts();
        return ResponseEntity.ok(accounts);
    }

    /**
     * Connect social account
     */
    @PostMapping("/connected-accounts/{provider}")
    public ResponseEntity<?> connectSocialAccount(
            @PathVariable String provider,
            @RequestBody SocialConnectRequest request) {
        try {
            accountService.connectCurrentUserSocialAccount(provider, request.getToken());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", provider + " account connected successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to connect " + provider + " account: " + e.getMessage()
            ));
        }
    }

    /**
     * Disconnect social account
     */
    @DeleteMapping("/connected-accounts/{provider}")
    public ResponseEntity<?> disconnectSocialAccount(@PathVariable String provider) {
        try {
            accountService.disconnectCurrentUserSocialAccount(provider);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", provider + " account disconnected successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to disconnect " + provider + " account: " + e.getMessage()
            ));
        }
    }

    /**
     * Export user data
     */
    @GetMapping("/data-export")
    public ResponseEntity<byte[]> exportUserData() {
        byte[] data = accountService.exportCurrentUserData();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "user-data.zip");

        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    /**
     * Delete user account
     */
    @DeleteMapping("/account")
    public ResponseEntity<?> deleteUserAccount() {
        try {
            accountService.deleteCurrentUserAccount();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Account deleted successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to delete account: " + e.getMessage()
            ));
        }
    }
}
