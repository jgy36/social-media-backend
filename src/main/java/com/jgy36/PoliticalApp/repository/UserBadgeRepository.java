package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    Optional<UserBadge> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
