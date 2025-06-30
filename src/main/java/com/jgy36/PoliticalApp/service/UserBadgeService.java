package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.dto.UserBadgeDto;
import com.jgy36.PoliticalApp.entity.UserBadge;
import com.jgy36.PoliticalApp.repository.UserBadgeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserBadgeService {

    private static final int MAX_BADGES = 10;
    private final UserBadgeRepository userBadgeRepository;

    @Autowired
    public UserBadgeService(UserBadgeRepository userBadgeRepository) {
        this.userBadgeRepository = userBadgeRepository;
    }

    public UserBadgeDto getUserBadges(Long userId) {
        Optional<UserBadge> userBadgeOpt = userBadgeRepository.findByUserId(userId);

        if (userBadgeOpt.isPresent()) {
            UserBadge userBadge = userBadgeOpt.get();
            return new UserBadgeDto(userId, userBadge.getBadgeIds());
        }

        return new UserBadgeDto(userId, new ArrayList<>());
    }

    @Transactional
    public UserBadgeDto saveBadges(Long userId, List<String> badgeIds) {
        // Validate badge count
        if (badgeIds.size() > MAX_BADGES) {
            throw new IllegalArgumentException("Maximum " + MAX_BADGES + " badges allowed");
        }

        Optional<UserBadge> existingBadges = userBadgeRepository.findByUserId(userId);
        UserBadge userBadge;

        if (existingBadges.isPresent()) {
            userBadge = existingBadges.get();
            userBadge.setBadgeIds(badgeIds);
        } else {
            userBadge = new UserBadge();
            userBadge.setUserId(userId);
            userBadge.setBadgeIds(badgeIds);
        }

        userBadge = userBadgeRepository.save(userBadge);
        return new UserBadgeDto(userId, userBadge.getBadgeIds());
    }

    @Transactional
    public void clearBadges(Long userId) {
        userBadgeRepository.deleteByUserId(userId);
    }
}
