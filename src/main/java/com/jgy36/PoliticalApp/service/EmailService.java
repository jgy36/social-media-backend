package com.jgy36.PoliticalApp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(String toEmail, String token) {
        logger.info("=== sendVerificationEmail START ===");
        logger.info("Recipient email: {}", toEmail);
        logger.info("Token: {}", token);
        logger.info("From email: {}", fromEmail);

        if (toEmail == null) {
            logger.error("toEmail is null!");
            throw new IllegalArgumentException("Email cannot be null");
        }

        if (token == null) {
            logger.error("Token is null!");
            throw new IllegalArgumentException("Token cannot be null");
        }

        // Generate a 6-digit verification code from the token
        String verificationCode = generateVerificationCode(token);
        logger.info("Generated verification code: {}", verificationCode);

        String subject = "Verify your account";
        String message = "Your verification code is: " + verificationCode +
                "\n\nEnter this code in the app to verify your account." +
                "\n\nThis code will expire in 24 hours." +
                "\n\nIf you didn't request this verification, please ignore this email.";

        try {
            logger.info("Calling sendEmail method...");
            sendEmail(toEmail, subject, message);
            logger.info("sendEmail completed successfully");
        } catch (Exception e) {
            logger.error("Exception in sendEmail: ", e);
            throw e;
        }

        logger.info("=== sendVerificationEmail END ===");
    }

    /**
     * Generate a 6-digit alphanumeric verification code from the token
     * @param token The verification token
     * @return A 6-character verification code
     */
    private String generateVerificationCode(String token) {
        // Take first 6 characters of token, remove dashes, and make uppercase
        String cleaned = token.replaceAll("-", "").toUpperCase();

        // Ensure we have at least 6 characters
        if (cleaned.length() >= 6) {
            return cleaned.substring(0, 6);
        } else {
            // Fallback: pad with numbers if needed
            return (cleaned + "123456").substring(0, 6);
        }
    }

    public void sendEmail(String to, String subject, String text) {
        logger.info("=== sendEmail START ===");
        logger.info("To: {}", to);
        logger.info("Subject: {}", subject);
        logger.info("From: {}", fromEmail);
        logger.info("Text length: {}", text != null ? text.length() : "null");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            logger.info("Sending mail message...");
            mailSender.send(message);
            logger.info("Mail sent successfully!");
        } catch (Exception e) {
            logger.error("Failed to send email: ", e);
            throw e;
        }

        logger.info("=== sendEmail END ===");
    }
}
