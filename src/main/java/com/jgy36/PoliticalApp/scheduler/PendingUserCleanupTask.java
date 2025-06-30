package com.jgy36.PoliticalApp.scheduler;

import com.jgy36.PoliticalApp.repository.PendingUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class PendingUserCleanupTask {
    @Autowired
    private PendingUserRepository pendingUserRepository;

    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    public void cleanupExpiredPendingUsers() {
        pendingUserRepository.deleteExpiredPendingUsers(LocalDateTime.now());
    }
}
