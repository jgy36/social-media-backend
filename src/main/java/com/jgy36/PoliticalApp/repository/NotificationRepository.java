package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.Notification;
import com.jgy36.PoliticalApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);

    // Add this new method to find unread notifications
    List<Notification> findByRecipientAndReadFalse(User recipient);
}
