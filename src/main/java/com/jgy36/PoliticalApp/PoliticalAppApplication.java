package com.jgy36.PoliticalApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.jgy36.PoliticalApp")
@EnableScheduling  // Add this annotation
@EnableAspectJAutoProxy  // ADD THIS LINE


public class PoliticalAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(PoliticalAppApplication.class, args);
    }

}
