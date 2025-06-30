package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/email")
    public ResponseEntity<?> testEmail(@RequestParam String to) {
        try {
            emailService.sendEmail(to, "Test Email", "This is a test email from your Political App!");
            return ResponseEntity.ok("Test email sent to: " + to);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
        }
    }
}
