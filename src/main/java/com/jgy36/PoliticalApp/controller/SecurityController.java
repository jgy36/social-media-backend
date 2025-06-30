package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.dto.ChangePasswordRequest;
import com.jgy36.PoliticalApp.dto.VerifyTwoFaRequest;
import com.jgy36.PoliticalApp.entity.UserSession;
import com.jgy36.PoliticalApp.service.SecurityService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class SecurityController {
    private final SecurityService securityService;

    public SecurityController(SecurityService securityService) {
        this.securityService = securityService;
    }

    /**
     * Get 2FA status for the current user
     */
    @GetMapping("/2fa/status")
    public ResponseEntity<?> getTwoFaStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = securityService.getCurrentUserSecuritySettings().getUserId();
        boolean is2faEnabled = securityService.isTwoFaEnabled(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("enabled", is2faEnabled);

        return ResponseEntity.ok(response);
    }

    /**
     * Setup 2FA for the current user
     */
    @PostMapping("/2fa/setup")
    public ResponseEntity<?> setupTwoFa() {
        String secret = securityService.generateTwoFaSecret();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        // Generate QR code URL with proper encoding
        String otpAuth = String.format(
                "otpauth://totp/PoliticalApp:%s?secret=%s&issuer=PoliticalApp&algorithm=SHA1&digits=6&period=30",
                URLEncoder.encode(userEmail, StandardCharsets.UTF_8),
                secret
        );

        // Use Google Charts API with proper URL encoding
        String qrCodeUrl = String.format(
                "https://chart.googleapis.com/chart?cht=qr&chs=300x300&chld=L|0&chl=%s",
                URLEncoder.encode(otpAuth, StandardCharsets.UTF_8)
        );

        Map<String, Object> response = new HashMap<>();
        response.put("secretKey", secret);
        response.put("qrCodeUrl", qrCodeUrl);

        return ResponseEntity.ok(response);
    }

    /**
     * Verify and enable 2FA
     */
    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verifyTwoFa(@RequestBody VerifyTwoFaRequest request) {
        boolean isCodeValid = securityService.verifyTwoFaCode(request.getSecret(), request.getCode());

        if (!isCodeValid) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid verification code"));
        }

        // Enable 2FA for the user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = securityService.getCurrentUserSecuritySettings().getUserId();
        securityService.enableTwoFa(userId, request.getSecret());

        return ResponseEntity.ok(Map.of("success", true, "message", "Two-factor authentication enabled successfully"));
    }

    /**
     * Disable 2FA
     */
    @DeleteMapping("/2fa")
    public ResponseEntity<?> disableTwoFa() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = securityService.getCurrentUserSecuritySettings().getUserId();
        securityService.disableTwoFa(userId);

        return ResponseEntity.ok(Map.of("success", true, "message", "Two-factor authentication disabled successfully"));
    }

    /**
     * Change password
     */
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = securityService.getCurrentUserSecuritySettings().getUserId();

        boolean success = securityService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());

        if (!success) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Current password is incorrect"));
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Password changed successfully"));
    }

    /**
     * Get active sessions for the current user
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getActiveSessions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = securityService.getCurrentUserSecuritySettings().getUserId();

        List<UserSession> sessions = securityService.getActiveSessions(userId);
        List<Map<String, Object>> sessionDtos = sessions.stream()
                .map(session -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", session.getId());
                    dto.put("browser", session.getBrowser());
                    dto.put("os", session.getOs());
                    dto.put("ipAddress", session.getIpAddress());
                    dto.put("location", session.getLocation());
                    dto.put("lastActive", session.getLastActive().toString());
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(sessionDtos);
    }

    /**
     * Logout from all sessions except the current one
     */
    @PostMapping("/sessions/logout-all")
    public ResponseEntity<?> logoutAllSessions(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = securityService.getCurrentUserSecuritySettings().getUserId();

        // Get the current session ID from request
        String currentSessionId = request.getSession().getId();

        securityService.terminateAllSessionsExceptCurrent(userId, currentSessionId);

        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out from all other devices"));
    }

    /**
     * Terminate a specific session
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> terminateSession(@PathVariable String sessionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = securityService.getCurrentUserSecuritySettings().getUserId();

        boolean success = securityService.terminateSession(userId, sessionId);

        if (!success) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Session not found or not authorized"));
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Session terminated successfully"));
    }
}
