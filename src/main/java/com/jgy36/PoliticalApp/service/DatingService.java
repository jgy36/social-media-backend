package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.dto.DatingPreferencesRequest;
import com.jgy36.PoliticalApp.entity.*;
import com.jgy36.PoliticalApp.repository.DatingProfileRepository;
import com.jgy36.PoliticalApp.repository.MatchRepository;
import com.jgy36.PoliticalApp.repository.SwipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class DatingService {

    @Autowired
    private DatingProfileRepository datingProfileRepository;

    @Autowired
    private SwipeRepository swipeRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    public DatingProfile createOrUpdateDatingProfile(User user, DatingProfile profileData) {
        Optional<DatingProfile> existingProfile = datingProfileRepository.findByUser(user);

        if (existingProfile.isPresent()) {
            DatingProfile profile = existingProfile.get();
            // Update existing profile with ALL fields
            profile.setBio(profileData.getBio());
            profile.setAge(profileData.getAge());
            profile.setLocation(profileData.getLocation());

            // Update NEW fields
            profile.setHeight(profileData.getHeight());
            profile.setJob(profileData.getJob());
            profile.setReligion(profileData.getReligion());
            profile.setRelationshipType(profileData.getRelationshipType());
            profile.setLifestyle(profileData.getLifestyle());

            // ✅ ADD THIS LINE - UPDATE GENDER
            profile.setGender(profileData.getGender());

            // ADD THESE NEW LINES for vitals & vices:
            profile.setHasChildren(profileData.getHasChildren());
            profile.setWantChildren(profileData.getWantChildren());
            profile.setDrinking(profileData.getDrinking());
            profile.setSmoking(profileData.getSmoking());
            profile.setDrugs(profileData.getDrugs());
            profile.setLookingFor(profileData.getLookingFor());
            profile.setInterests(profileData.getInterests());
            profile.setVirtues(profileData.getVirtues());

            // Update existing fields
            profile.setPhotos(profileData.getPhotos());

            // ONLY update gender preference if it's not null (user set it)
            if (profileData.getGenderPreference() != null) {
                profile.setGenderPreference(profileData.getGenderPreference());
            }

            // ONLY update these if they're not null
            if (profileData.getMinAge() != null) {
                profile.setMinAge(profileData.getMinAge());
            }
            if (profileData.getMaxAge() != null) {
                profile.setMaxAge(profileData.getMaxAge());
            }
            if (profileData.getMaxDistance() != null) {
                profile.setMaxDistance(profileData.getMaxDistance());
            }

            // Update prompts
            profile.setPrompts(profileData.getPrompts());

            return datingProfileRepository.save(profile);
        } else {
            // Create new profile
            profileData.setUser(user);
            profileData.setIsActive(true);

            // Set default values for preferences if not provided
            if (profileData.getMinAge() == null) {
                profileData.setMinAge(18);
            }
            if (profileData.getMaxAge() == null) {
                profileData.setMaxAge(100);
            }
            if (profileData.getMaxDistance() == null) {
                profileData.setMaxDistance(50);
            }

            return datingProfileRepository.save(profileData);
        }
    }

    // Update dating preferences method
    public DatingProfile updateDatingPreferences(User user, DatingPreferencesRequest request) {
        DatingProfile profile = getDatingProfileByUser(user);

        if (profile == null) {
            throw new RuntimeException("No dating profile found for user");
        }

        // Update preferences
        if (request.getGenderPreference() != null) {
            profile.setGenderPreference(request.getGenderPreference());
        }
        if (request.getMinAge() != null) {
            profile.setMinAge(request.getMinAge());
        }
        if (request.getMaxAge() != null) {
            profile.setMaxAge(request.getMaxAge());
        }
        if (request.getMaxDistance() != null) {
            profile.setMaxDistance(request.getMaxDistance());
        }

        return datingProfileRepository.save(profile);
    }

    // Update getPotentialMatches in DatingService.java
    // ✅ UPDATED: Convert enum to string for repository query
    public List<DatingProfile> getPotentialMatches(User user) {
        DatingProfile userProfile = getDatingProfileByUser(user);

        if (userProfile == null || !user.isEligibleForDating()) {
            return new ArrayList<>();
        }

        // Convert GenderPreference enum to string for the query
        String genderPreferenceStr = null;
        if (userProfile.getGenderPreference() != null) {
            genderPreferenceStr = userProfile.getGenderPreference().name();
        }

        // If no preference is set, default to showing everyone
        if (genderPreferenceStr == null) {
            genderPreferenceStr = "EVERYONE";
        }

        System.out.println("🔍 Searching for matches with preferences:");
        System.out.println("  User ID: " + user.getId());
        System.out.println("  Gender Preference: " + genderPreferenceStr);
        System.out.println("  Age Range: " + userProfile.getMinAge() + "-" + userProfile.getMaxAge());

        // Get users based on gender preference and age range
        List<DatingProfile> matches = datingProfileRepository.findPotentialMatches(
                user.getId(),
                genderPreferenceStr,  // ✅ Pass as string instead of enum
                userProfile.getMinAge(),
                userProfile.getMaxAge()
        );

        System.out.println("📊 Found " + matches.size() + " potential matches");
        return matches;
    }

    public Match swipeUser(User swiper, User target, SwipeDirection direction) {
        // Check if already swiped
        if (swipeRepository.existsBySwiperAndTarget(swiper, target)) {
            throw new RuntimeException("Already swiped on this user");
        }

        // Record the swipe
        Swipe swipe = new Swipe();
        swipe.setSwiper(swiper);
        swipe.setTarget(target);
        swipe.setDirection(direction);
        swipe.setSwipedAt(LocalDateTime.now());
        swipeRepository.save(swipe);

        // Check for match if it was a LIKE
        if (direction == SwipeDirection.LIKE) {
            Optional<Swipe> reciprocalSwipe = swipeRepository.findBySwiperAndTarget(target, swiper);
            if (reciprocalSwipe.isPresent() && reciprocalSwipe.get().getDirection() == SwipeDirection.LIKE) {
                // It's a match!
                Match match = new Match();
                match.setUser1(swiper);
                match.setUser2(target);
                match.setMatchedAt(LocalDateTime.now());
                match.setIsActive(true);
                return matchRepository.save(match);
            }
        }

        return null; // No match
    }

    public List<Match> getUserMatches(User user) {
        return matchRepository.findActiveMatchesForUser(user);
    }

    public DatingProfile getDatingProfileByUser(User user) {
        return datingProfileRepository.findByUser(user).orElse(null);
    }

    public long getTotalDatingProfileCount() {
        return datingProfileRepository.count();
    }

    /**
     * Mark a match as seen by a user (for removing "new match" indicators)
     */
    public void markMatchAsSeen(Long matchId, User user) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        // Verify user is part of this match
        if (!match.getUser1().getId().equals(user.getId()) &&
                !match.getUser2().getId().equals(user.getId())) {
            throw new RuntimeException("User is not part of this match");
        }

        // You could add a "seenByUser1" and "seenByUser2" field to Match entity
        // For now, this is just a placeholder - you could track this in Redis or another way

        // Or simply update a "lastInteractionAt" field to indicate activity
        // This would be used by the "isNewMatch" logic to determine if it's still "new"
    }

    /**
     * Check if two users are matched
     */
    public boolean areUsersMatched(User user1, User user2) {
        List<Match> matches = matchRepository.findActiveMatchesBetweenUsers(user1.getId(), user2.getId());
        return !matches.isEmpty();
    }

    /**
     * Get users who liked the current user
     */
    // Update this method in DatingService.java
    public List<DatingProfile> getWhoLikedMe(User user) {
        Subscription subscription = subscriptionService.getCurrentUserSubscription();

        if (subscription.getTier() == SubscriptionTier.FREE) {
            throw new SubscriptionRequiredException("Upgrade to see who liked you");
        }

        // Get all users who swiped LIKE on current user
        List<Swipe> likesReceived = swipeRepository.findLikesReceivedByUser(user);

        // Convert to profiles and filter out users who are already matched
        List<DatingProfile> profiles = likesReceived.stream()
                .map(swipe -> getDatingProfileByUser(swipe.getSwiper()))
                .filter(Objects::nonNull)
                .filter(profile -> !areUsersMatched(user, profile.getUser())) // ADD THIS LINE
                .collect(Collectors.toList());

        System.out.println("🔍 Likes before filtering: " + likesReceived.size());
        System.out.println("🔍 Likes after filtering matched users: " + profiles.size());

        // ESSENTIAL tier: only show last 5 likes
        if (subscription.getTier() == SubscriptionTier.ESSENTIAL) {
            return profiles.stream()
                    .limit(5)
                    .collect(Collectors.toList());
        }

        // PREMIUM+ tiers: show all likes
        return profiles;
    }

    public Match likeUserBack(User swiper, User target) {
        // First, verify that the target user has actually liked the swiper
        Optional<Swipe> targetLikedSwiper = swipeRepository.findBySwiperAndTarget(target, swiper);

        if (targetLikedSwiper.isEmpty() || targetLikedSwiper.get().getDirection() != SwipeDirection.LIKE) {
            throw new RuntimeException("Target user has not liked you");
        }

        // Check if swiper has already swiped on target
        Optional<Swipe> existingSwipe = swipeRepository.findBySwiperAndTarget(swiper, target);

        if (existingSwipe.isPresent()) {
            // Update existing swipe to LIKE
            Swipe swipe = existingSwipe.get();
            swipe.setDirection(SwipeDirection.LIKE);
            swipe.setSwipedAt(LocalDateTime.now());
            swipeRepository.save(swipe);
        } else {
            // Create new LIKE swipe
            Swipe swipe = new Swipe();
            swipe.setSwiper(swiper);
            swipe.setTarget(target);
            swipe.setDirection(SwipeDirection.LIKE);
            swipe.setSwipedAt(LocalDateTime.now());
            swipeRepository.save(swipe);
        }

        // Since both users have liked each other, create a match
        // Check if match already exists
        List<Match> existingMatches = matchRepository.findActiveMatchesBetweenUsers(swiper.getId(), target.getId());
        if (!existingMatches.isEmpty()) {
            return existingMatches.get(0); // Return existing match
        }

        // Create new match
        Match match = new Match();
        match.setUser1(swiper);
        match.setUser2(target);
        match.setMatchedAt(LocalDateTime.now());
        match.setIsActive(true);
        return matchRepository.save(match);
    }
}
