package com.jgy36.PoliticalApp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Get the current working directory
        String currentDir = System.getProperty("user.dir");
        String uploadPath = "file:" + currentDir + "/uploads/";

        System.out.println("🌐 Configuring static resource handler for uploads");
        System.out.println("📂 Current directory: " + currentDir);
        System.out.println("📂 Upload path: " + uploadPath);

        // Create uploads directory if it doesn't exist
        File uploadDir = new File(currentDir + "/uploads");
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            System.out.println("📁 Created directory: " + uploadDir.getAbsolutePath() + " - " + (created ? "Success" : "Failed"));
        }

        // Create profile-images subdirectory
        File profileImagesDir = new File(currentDir + "/uploads/profile-images");
        if (!profileImagesDir.exists()) {
            boolean created = profileImagesDir.mkdirs();
            System.out.println("📁 Created directory: " + profileImagesDir.getAbsolutePath() + " - " + (created ? "Success" : "Failed"));
        }

        // Register the uploads directory with its absolute path
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);

        System.out.println("✅ Resource handler configured: /uploads/** -> " + uploadPath);
    }
}
