package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.UserSecuritySettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSecuritySettingsRepository extends JpaRepository<UserSecuritySettings, Long> {
    Optional<UserSecuritySettings> findByUserId(Long userId);
}
