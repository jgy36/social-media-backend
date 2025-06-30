package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.config.JwtTokenUtil;
import com.jgy36.PoliticalApp.dto.AuthResponse;
import com.jgy36.PoliticalApp.dto.LoginRequest;
import com.jgy36.PoliticalApp.dto.RegisterRequest;
import com.jgy36.PoliticalApp.dto.TwoFAVerificationRequest;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.entity.UserSecuritySettings;
import com.jgy36.PoliticalApp.repository.UserRepository;
import com.jgy36.PoliticalApp.service.SecurityService;
import com.jgy36.PoliticalApp.service.TokenBlacklistService;
import com.jgy36.PoliticalApp.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
        origins = "http://localhost:3000",
        allowCredentials = "true",
        allowedHeaders = {
                "Authorization", "Content-Type", "Accept", "X-Requested-With",
                "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers",
                "Cache-Control", "Pragma", "Expires"
        },
        exposedHeaders = {"Authorization"}
)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecurityService securityService;


    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtTokenUtil jwtTokenUtil,
                          TokenBlacklistService tokenBlacklistService,
                          UserRepository userRepository,
                          BCryptPasswordEncoder passwordEncoder,
                          SecurityService securityService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityService = securityService;
    }

    /**
     * ✅ Register a new user.
     */
    /**
     * ✅ Register a new user.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletResponse response) {
        try {
            // Create pending user instead of actual user
            userService.createPendingUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getDisplayName()
            );

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Registration successful! Please check your email to verify your account.");
            responseData.put("email", request.getEmail());

            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "Registration failed: " + e.getMessage()
                    )
            );
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            // First try to verify a pending user
            User user = userService.verifyAndCreateUser(token);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Email verified successfully! You can now login."
            ));
        } catch (IllegalArgumentException e) {
            // If not found in pending users, try existing users (backward compatibility)
            try {
                User existingUser = userRepository.findByVerificationToken(token)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

                // Check if token is expired
                if (existingUser.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Verification token has expired. Please register again."
                    ));
                }

                // Verify the user
                existingUser.setVerified(true);
                existingUser.setVerificationToken(null);
                existingUser.setVerificationTokenExpiresAt(null);
                userRepository.save(existingUser);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Email verified successfully! You can now login."
                ));
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Error verifying email: " + ex.getMessage()
                ));
            }
        }
    }


    /**
     * ✅ Login endpoint: Authenticates user and returns JWT token with complete profile data.
     */
    // In AuthController.java, modify the loginUser method:
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        // First check if user exists and is verified
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // TEMPORARILY DISABLED - Comment out or remove this block to allow unverified users to login
    /*
    // Check if user is verified
    if (!user.isVerified()) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "Please verify your email before logging in.",
                "errorCode", "EMAIL_NOT_VERIFIED"
        ));
    }
    */

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Rest of the login method remains the same...
        // Check if 2FA is enabled for this user
        if (securityService.isTwoFaEnabled(user.getId())) {
            // Generate a temporary token for 2FA verification
            String tempToken = jwtTokenUtil.generateToken(user.getEmail(), 300); // 5 minutes validity

            // Return response indicating 2FA is required
            Map<String, Object> twoFaResponse = new HashMap<>();
            twoFaResponse.put("requires2FA", true);
            twoFaResponse.put("tempToken", tempToken);
            twoFaResponse.put("message", "Please enter your 2FA verification code");

            return ResponseEntity.ok(twoFaResponse);
        }

        // If no 2FA, continue with normal login flow
        String username = ((org.springframework.security.core.userdetails.User) authentication.getPrincipal()).getUsername();
        String token = jwtTokenUtil.generateToken(username);

        // Set JWT in HTTP-only cookie
        Cookie jwtCookie = new Cookie("jwt", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(24 * 60 * 60);
        response.addCookie(jwtCookie);

        // Add session identifier cookie
        String sessionId = UUID.randomUUID().toString();
        Cookie sessionCookie = new Cookie("session_id", sessionId);
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(24 * 60 * 60);
        response.addCookie(sessionCookie);

        response.setHeader("Authorization", "Bearer " + token);

        // Return complete user info
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put("username", user.getUsername());
        userResponse.put("email", user.getEmail());
        userResponse.put("displayName", user.getDisplayName());
        userResponse.put("bio", user.getBio());
        userResponse.put("profileImageUrl", user.getProfileImageUrl());

        Map<String, Object> fullResponse = new HashMap<>();
        fullResponse.put("token", token);
        fullResponse.put("user", userResponse);
        fullResponse.put("sessionId", sessionId);
        fullResponse.put("requires2FA", false);

        return ResponseEntity.ok(fullResponse);
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verify2FA(@RequestBody TwoFAVerificationRequest request, HttpServletResponse response) {
        try {
            // Validate the temporary token
            String email = jwtTokenUtil.getUsernameFromToken(request.getTempToken());

            // Verify token hasn't expired
            if (jwtTokenUtil.isTokenExpired(request.getTempToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "Verification token has expired"
                ));
            }

            // Get user
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Get user's 2FA settings
            UserSecuritySettings settings = securityService.getUserSecuritySettings(user.getId());

            // Verify the 2FA code
            if (!securityService.verifyTwoFaCode(settings.getTwoFaSecret(), request.getCode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "Invalid verification code"
                ));
            }

            // Generate real JWT token
            String token = jwtTokenUtil.generateToken(email);

            // Set JWT in HTTP-only cookie
            Cookie jwtCookie = new Cookie("jwt", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(false);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(24 * 60 * 60);
            response.addCookie(jwtCookie);

            // Add session identifier cookie
            String sessionId = UUID.randomUUID().toString();
            Cookie sessionCookie = new Cookie("session_id", sessionId);
            sessionCookie.setPath("/");
            sessionCookie.setMaxAge(24 * 60 * 60);
            response.addCookie(sessionCookie);

            response.setHeader("Authorization", "Bearer " + token);

            // Return complete user info
            Map<String, Object> userResponse = new HashMap<>();
            userResponse.put("id", user.getId());
            userResponse.put("username", user.getUsername());
            userResponse.put("email", user.getEmail());
            userResponse.put("displayName", user.getDisplayName());
            userResponse.put("bio", user.getBio());
            userResponse.put("profileImageUrl", user.getProfileImageUrl());

            Map<String, Object> fullResponse = new HashMap<>();
            fullResponse.put("token", token);
            fullResponse.put("user", userResponse);
            fullResponse.put("sessionId", sessionId);
            fullResponse.put("success", true);

            return ResponseEntity.ok(fullResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Invalid or expired verification token"
            ));
        }
    }

    /**
     * ✅ Logout User by Invalidating Token and clearing cookies
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // Get token from cookie
        Cookie[] cookies = request.getCookies();
        String token = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // If no cookie, try Authorization header
        if (token == null) {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        // Blacklist the token if we found one
        if (token != null) {
            long expiration = jwtTokenUtil.getExpirationFromToken(token);
            tokenBlacklistService.blacklistToken(token, expiration - System.currentTimeMillis());
        }

        // Clear cookies regardless
        Cookie jwtCookie = new Cookie("jwt", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0); // Delete the cookie
        response.addCookie(jwtCookie);

        Cookie sessionCookie = new Cookie("session_id", null);
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(0); // Delete the cookie
        response.addCookie(sessionCookie);

        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/google-login")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody Map<String, String> userData, HttpServletResponse response) {
        String email = userData.get("email");
        String name = userData.get("name");

        // ✅ Check if user already exists
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            // ✅ If user doesn't exist, create a new one with a random password
            user = new User();
            user.setEmail(email);
            user.setUsername(name);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Random password
            userRepository.save(user);
        }

        // ✅ Generate JWT token for authentication
        String token = jwtTokenUtil.generateToken(user.getEmail());

        // ✅ Set JWT in HTTP-only cookie
        Cookie jwtCookie = new Cookie("jwt", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false); // Set to true in production with HTTPS
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(24 * 60 * 60); // 24 hours in seconds
        response.addCookie(jwtCookie);

        // ✅ Add a session identifier cookie (not HTTP-only so JS can read it)
        String sessionId = UUID.randomUUID().toString();
        Cookie sessionCookie = new Cookie("session_id", sessionId);
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(24 * 60 * 60); // 24 hours
        response.addCookie(sessionCookie);

        // Return token for API clients
        AuthResponse authResponse = new AuthResponse(token);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Improved refresh token endpoint that includes full user data
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // Get token from cookie
        Cookie[] cookies = request.getCookies();
        String token = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // If no cookie, try Authorization header
        if (token == null) {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No valid token provided");
            }
            token = authHeader.substring(7);
        }

        try {
            // Validate existing token before refreshing
            String username = jwtTokenUtil.getUsernameFromToken(token);

            // Check if token is blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token has been invalidated");
            }

            // Check if token is expired
            if (jwtTokenUtil.isTokenExpired(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token has expired");
            }

            // Generate new token
            String newToken = jwtTokenUtil.generateToken(username);

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // ✅ Set new JWT in HTTP-only cookie
            Cookie jwtCookie = new Cookie("jwt", newToken);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(false); // Set to true in production with HTTPS
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(24 * 60 * 60); // 24 hours in seconds
            response.addCookie(jwtCookie);

            // ✅ Set a new session ID
            String sessionId = UUID.randomUUID().toString();
            Cookie sessionCookie = new Cookie("session_id", sessionId);
            sessionCookie.setPath("/");
            sessionCookie.setMaxAge(24 * 60 * 60); // 24 hours
            response.addCookie(sessionCookie);

            // Also set the new token as Authorization header for API clients
            response.setHeader("Authorization", "Bearer " + newToken);

            // Return both token & detailed user info
            Map<String, Object> userResponse = new HashMap<>();
            userResponse.put("id", user.getId());
            userResponse.put("username", user.getUsername());
            userResponse.put("email", user.getEmail());
            userResponse.put("displayName", user.getDisplayName());
            userResponse.put("bio", user.getBio());
            userResponse.put("profileImageUrl", user.getProfileImageUrl());

            Map<String, Object> fullResponse = new HashMap<>();
            fullResponse.put("token", newToken);
            fullResponse.put("user", userResponse);
            fullResponse.put("sessionId", sessionId);

            return ResponseEntity.ok(fullResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
    }

    /**
     * Check username availability via POST with detailed logging
     */
    @PostMapping("/check-username")
    @CrossOrigin(
            origins = "http://localhost:3000",
            allowCredentials = "true",
            allowedHeaders = {
                    "Authorization", "Content-Type", "Accept", "X-Requested-With",
                    "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers",
                    "Cache-Control", "Pragma", "Expires"
            },
            exposedHeaders = {"Authorization"}
    )
    public ResponseEntity<?> checkUsernameAvailabilityPost(@RequestBody Map<String, String> request) {
        logger.info("✅ Received username check request: " + request);
        try {
            String username = request.get("username");
            if (username == null || username.isEmpty()) {
                logger.warn("❌ Username check failed: username is null or empty");
                return ResponseEntity.badRequest().body(Map.of(
                        "available", false,
                        "message", "Username is required"
                ));
            }

            logger.info("🔍 Checking if username exists: " + username);
            boolean exists = userRepository.existsByUsernameIgnoreCase(username);
            logger.info("🔍 Username exists check result: " + exists);

            Map<String, Object> response = new HashMap<>();
            response.put("available", !exists);

            if (exists) {
                response.put("message", "Username already exists. Please choose another.");
            }

            logger.info("✅ Username check completed successfully: " + response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error checking username availability: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error checking username availability: " + e.getMessage()));
        }
    }
    // Add this method to your existing AuthController class

    @PostMapping("/verify")  // Remove /api/auth prefix
    public ResponseEntity<?> verifyEmailByCode(@RequestParam String code, HttpServletResponse response) {
        logger.info("=== EMAIL VERIFICATION ENDPOINT CALLED ===");
        logger.info("Verification code received: {}", code);

        try {
            // Call your user service to verify the CODE
            boolean isVerified = userService.verifyEmailCode(code);

            if (isVerified) {
                logger.info("Email verification successful for code: {}", code);

                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("success", true);
                successResponse.put("message", "Account verified successfully! You can now log in.");

                return ResponseEntity.ok(successResponse);
            } else {
                logger.warn("Email verification failed - invalid or expired code: {}", code);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid or expired verification code.");

                return ResponseEntity.badRequest().body(errorResponse);
            }

        } catch (Exception e) {
            logger.error("Error during email verification: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Verification failed. Please try again.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

}
