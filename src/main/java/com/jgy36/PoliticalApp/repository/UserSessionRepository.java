package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    List<UserSession> findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.user.id = :userId AND s.id != :currentSessionId")
    void deleteAllExceptCurrentByUserId(Long userId, String currentSessionId);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.user.id = :userId")
    void deleteAllByUserId(Long userId);
}
