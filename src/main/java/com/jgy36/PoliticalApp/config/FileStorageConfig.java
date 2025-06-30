package com.jgy36.PoliticalApp.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FileStorageConfig {

    private final String uploadDir = "uploads/media";

    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(uploadDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("Created directory: " + path.toAbsolutePath());
            } else {
                System.out.println("Directory already exists: " + path.toAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize storage directory: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }
}
