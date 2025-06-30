package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.entity.ConnectedAccount;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.repository.ConnectedAccountRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class AccountManagementService {
    private final UserRepository userRepository;
    private final ConnectedAccountRepository connectedAccountRepository;
    private final JavaMailSender mailSender;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final EmailService emailService;  // ADD THIS LINE


    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public AccountManagementService(
            UserRepository userRepository,
            ConnectedAccountRepository connectedAccountRepository,
            JavaMailSender mailSender,
            OAuth2AuthorizedClientService authorizedClientService, EmailService emailService) {
        this.userRepository = userRepository;
        this.connectedAccountRepository = connectedAccountRepository;
        this.mailSender = mailSender;
        this.authorizedClientService = authorizedClientService;
        this.emailService = emailService;  // ADD THIS LINE
    }

    /**
     * Get the current authenticated user's ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return user.getId();
    }

    /**
     * Check if email is verified for a user
     */
    public boolean isEmailVerified(Long userId) {
        return userRepository.findById(userId)
                .map(User::isEmailVerified)
                .orElse(false);
    }

    /**
     * Check if current user's email is verified
     */
    public boolean isCurrentUserEmailVerified() {
        return isEmailVerified(getCurrentUserId());
    }

    /**
     * Generate a verification token
     */
    private String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Send verification email to user
     */
    public void sendVerificationEmail(User user) throws MessagingException {
        System.out.println("=== AccountManagementService.sendVerificationEmail START ===");
        System.out.println("User: " + user);
        System.out.println("User email: " + user.getEmail());
        System.out.println("User token (before): " + user.getVerificationToken());

        // Generate token if not exists
        if (user.getVerificationToken() == null) {
            System.out.println("Generating new verification token");
            user.setVerificationToken(generateVerificationToken());
            user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
            userRepository.save(user);
            System.out.println("User token (after): " + user.getVerificationToken());
        }

        // Use EmailService to send the email
        try {
            System.out.println("Calling EmailService.sendVerificationEmail...");
            emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());
            System.out.println("EmailService.sendVerificationEmail completed");
        } catch (Exception e) {
            System.err.println("Error calling EmailService: " + e.getMessage());
            e.printStackTrace();
            // Wrap any exception as MessagingException to maintain the method signature
            if (e instanceof MessagingException) {
                throw (MessagingException) e;
            } else {
                throw new MessagingException("Failed to send verification email", e);
            }
        }

        System.out.println("=== AccountManagementService.sendVerificationEmail END ===");
    }

    /**
     * Send verification email to current user
     */
    public void sendVerificationEmailToCurrentUser() throws MessagingException {
        User user = userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        sendVerificationEmail(user);
    }

    /**
     * Verify email with token
     */
    @Transactional
    public boolean verifyEmail(Long userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getVerificationToken() == null || !user.getVerificationToken().equals(token)) {
            return false;
        }

        if (user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);

        return true;
    }

    /**
     * Verify email with token for current user
     */
    @Transactional
    public boolean verifyCurrentUserEmail(String token) {
        return verifyEmail(getCurrentUserId(), token);
    }

    /**
     * Request email change
     */
    @Transactional
    public void requestEmailChange(Long userId, String newEmail) throws MessagingException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Check if email is already in use
        if (userRepository.findByEmail(newEmail).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        // Generate token
        String token = generateVerificationToken();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        // Send verification email
        String verificationUrl = frontendUrl + "/change-email?token=" + token + "&email=" + newEmail;

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

        helper.setFrom(fromEmail);
        helper.setTo(newEmail);
        helper.setSubject("Confirm Your New Email Address");

        String htmlContent = "<html><body>"
                + "<h2>Email Change Request</h2>"
                + "<p>Hello " + user.getUsername() + ",</p>"
                + "<p>Please click the link below to confirm your new email address:</p>"
                + "<p><a href='" + verificationUrl + "'>Confirm Email Change</a></p>"
                + "<p>This link will expire in 24 hours.</p>"
                + "<p>If you did not request this change, please ignore this email.</p>"
                + "</body></html>";

        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    /**
     * Confirm email change
     */
    @Transactional
    public boolean confirmEmailChange(Long userId, String token, String newEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getVerificationToken() == null || !user.getVerificationToken().equals(token)) {
            return false;
        }

        if (user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Check if email is already in use
        if (userRepository.findByEmail(newEmail).isPresent()) {
            return false;
        }

        user.setEmail(newEmail);
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);

        return true;
    }

    /**
     * Request email change for current user
     */
    @Transactional
    public void requestCurrentUserEmailChange(String newEmail) throws MessagingException {
        requestEmailChange(getCurrentUserId(), newEmail);
    }

    /**
     * Get connected accounts for a user
     */
    public Map<String, Boolean> getConnectedAccounts(Long userId) {
        List<ConnectedAccount> accounts = connectedAccountRepository.findByUserId(userId);

        Map<String, Boolean> result = new HashMap<>();
        result.put("google", false);
        result.put("facebook", false);
        result.put("twitter", false);
        result.put("apple", false);

        for (ConnectedAccount account : accounts) {
            result.put(account.getProvider().toLowerCase(), true);
        }

        return result;
    }

    /**
     * Get connected accounts for current user
     */
    public Map<String, Boolean> getCurrentUserConnectedAccounts() {
        return getConnectedAccounts(getCurrentUserId());
    }

    /**
     * Connect social account
     */
    @Transactional
    public void connectSocialAccount(Long userId, String provider, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Check if already connected
        Optional<ConnectedAccount> existingAccount = connectedAccountRepository.findByUserIdAndProvider(userId, provider);

        if (existingAccount.isPresent()) {
            // Update existing account
            ConnectedAccount account = existingAccount.get();
            account.setAccessToken(token);
            account.setExpiresAt(LocalDateTime.now().plusDays(60)); // Token typically valid for 60 days
            connectedAccountRepository.save(account);
        } else {
            // Create new account
            ConnectedAccount account = new ConnectedAccount();
            account.setUser(user);
            account.setProvider(provider);
            account.setProviderUserId("placeholder"); // This would be set from OAuth response
            account.setAccessToken(token);
            account.setExpiresAt(LocalDateTime.now().plusDays(60));
            connectedAccountRepository.save(account);
        }
    }

    /**
     * Connect social account for current user
     */
    @Transactional
    public void connectCurrentUserSocialAccount(String provider, String token) {
        connectSocialAccount(getCurrentUserId(), provider, token);
    }

    /**
     * Disconnect social account
     */
    @Transactional
    public void disconnectSocialAccount(Long userId, String provider) {
        Optional<ConnectedAccount> account = connectedAccountRepository.findByUserIdAndProvider(userId, provider);
        account.ifPresent(connectedAccountRepository::delete);
    }

    /**
     * Disconnect social account for current user
     */
    @Transactional
    public void disconnectCurrentUserSocialAccount(String provider) {
        disconnectSocialAccount(getCurrentUserId(), provider);
    }

    /**
     * Export user data
     */
    public byte[] exportUserData(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            // Export user profile
            String userProfile = "User Profile:\n" +
                    "Username: " + user.getUsername() + "\n" +
                    "Email: " + user.getEmail() + "\n" +
                    "Display Name: " + (user.getDisplayName() != null ? user.getDisplayName() : "") + "\n" +
                    "Bio: " + (user.getBio() != null ? user.getBio() : "");

            ZipEntry profileEntry = new ZipEntry("profile.txt");
            zos.putNextEntry(profileEntry);
            zos.write(userProfile.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Add account settings
            Map<String, Boolean> connectedAccounts = getConnectedAccounts(userId);
            StringBuilder accountSettings = new StringBuilder("Connected Accounts:\n");
            for (Map.Entry<String, Boolean> entry : connectedAccounts.entrySet()) {
                accountSettings.append(entry.getKey()).append(": ").append(entry.getValue() ? "Connected" : "Not Connected").append("\n");
            }

            ZipEntry accountSettingsEntry = new ZipEntry("account_settings.txt");
            zos.putNextEntry(accountSettingsEntry);
            zos.write(accountSettings.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Add user posts and other data here...

            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export user data", e);
        }
    }

    /**
     * Export current user data
     */
    public byte[] exportCurrentUserData() {
        return exportUserData(getCurrentUserId());
    }

    /**
     * Delete user account
     */
    @Transactional
    public void deleteUserAccount(Long userId) {
        // Delete connected accounts
        connectedAccountRepository.deleteAllByUserId(userId);

        // Delete user
        userRepository.deleteById(userId);
    }

    /**
     * Delete current user account
     */
    @Transactional
    public void deleteCurrentUserAccount() {
        deleteUserAccount(getCurrentUserId());
    }
}
