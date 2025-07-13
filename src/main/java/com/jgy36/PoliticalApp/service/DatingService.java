package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.dto.DatingFilters;
import com.jgy36.PoliticalApp.dto.DatingPreferencesRequest;
import com.jgy36.PoliticalApp.entity.*;
import com.jgy36.PoliticalApp.repository.DatingProfileRepository;
import com.jgy36.PoliticalApp.repository.MatchRepository;
import com.jgy36.PoliticalApp.repository.SwipeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DatingService {

    private static final Logger logger = LoggerFactory.getLogger(DatingService.class);

    @Autowired
    private DatingProfileRepository datingProfileRepository;

    @Autowired
    private SwipeRepository swipeRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    // ==================== PROFILE MANAGEMENT ====================

    public DatingProfile createOrUpdateDatingProfile(User user, DatingProfile profileData) {
        Optional<DatingProfile> existingProfile = datingProfileRepository.findByUser(user);

        if (existingProfile.isPresent()) {
            DatingProfile profile = existingProfile.get();

            // Update all fields
            profile.setBio(profileData.getBio());
            profile.setAge(profileData.getAge());
            profile.setLocation(profileData.getLocation());
            profile.setHeight(profileData.getHeight());
            profile.setJob(profileData.getJob());
            profile.setReligion(profileData.getReligion());
            profile.setRelationshipType(profileData.getRelationshipType());
            profile.setLifestyle(profileData.getLifestyle());
            profile.setGender(profileData.getGender());

            // Vitals and vices
            profile.setHasChildren(profileData.getHasChildren());
            profile.setWantChildren(profileData.getWantChildren());
            profile.setDrinking(profileData.getDrinking());
            profile.setSmoking(profileData.getSmoking());
            profile.setDrugs(profileData.getDrugs());
            profile.setLookingFor(profileData.getLookingFor());
            profile.setInterests(profileData.getInterests());
            profile.setVirtues(profileData.getVirtues());
            profile.setPhotos(profileData.getPhotos());
            profile.setPrompts(profileData.getPrompts());

            // Only update preferences if not null
            if (profileData.getGenderPreference() != null) {
                profile.setGenderPreference(profileData.getGenderPreference());
            }
            if (profileData.getMinAge() != null) {
                profile.setMinAge(profileData.getMinAge());
            }
            if (profileData.getMaxAge() != null) {
                profile.setMaxAge(profileData.getMaxAge());
            }
            if (profileData.getMaxDistance() != null) {
                profile.setMaxDistance(profileData.getMaxDistance());
            }

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

            // Initialize algorithm fields
            if (profileData.getEloScore() == null) {
                profileData.setEloScore(1000);
            }
            if (profileData.getIsFreshProfile() == null) {
                profileData.setIsFreshProfile(true);
            }
            if (profileData.getTotalLikesReceived() == null) {
                profileData.setTotalLikesReceived(0);
            }
            if (profileData.getTotalSwipesReceived() == null) {
                profileData.setTotalSwipesReceived(0);
            }

            return datingProfileRepository.save(profileData);
        }
    }

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

    public DatingProfile getDatingProfileByUser(User user) {
        return datingProfileRepository.findByUser(user).orElse(null);
    }

    public long getTotalDatingProfileCount() {
        return datingProfileRepository.count();
    }

    // ==================== MATCHING & DISCOVERY ====================

    public List<DatingProfile> getPotentialMatches(User user) {
        return getPotentialMatches(user, null);
    }

    public List<DatingProfile> getPotentialMatches(User user, String location) {
        DatingFilters filters = new DatingFilters();
        filters.setLocation(location);
        return getPotentialMatchesWithFilters(user, filters);
    }

    public List<DatingProfile> getPotentialMatchesWithFilters(User user, DatingFilters filters) {
        DatingProfile userProfile = getDatingProfileByUser(user);

        if (userProfile == null || !user.isEligibleForDating()) {
            return new ArrayList<>();
        }

        String genderPreferenceStr = userProfile.getGenderPreference() != null ?
                userProfile.getGenderPreference().name() : "EVERYONE";

        String searchLocation = filters.getLocation() != null ? filters.getLocation() : userProfile.getLocation();

        logger.debug("Searching for matches with preferences:");
        logger.debug("User ID: {}", user.getId());
        logger.debug("Gender Preference: {}", genderPreferenceStr);
        logger.debug("Age Range: {}-{}", userProfile.getMinAge(), userProfile.getMaxAge());
        logger.debug("Location: {}", searchLocation);
        logger.debug("Filters applied: {}", filters.hasFilters());

        List<DatingProfile> matches;

        if (filters.hasFilters()) {
            matches = datingProfileRepository.findPotentialMatchesWithFilters(
                    user.getId(),
                    genderPreferenceStr,
                    userProfile.getMinAge(),
                    userProfile.getMaxAge(),
                    searchLocation,
                    filters.getLifestyle(),
                    filters.getReligion(),
                    filters.getRelationshipType(),
                    filters.getDrinking(),
                    filters.getSmoking(),
                    filters.getHasChildren(),
                    filters.getWantChildren()
            );
        } else {
            matches = datingProfileRepository.findPotentialMatches(
                    user.getId(),
                    genderPreferenceStr,
                    userProfile.getMinAge(),
                    userProfile.getMaxAge()
            );
        }

        logger.debug("Found {} potential matches", matches.size());
        return matches;
    }

    public List<DatingProfile> getPotentialMatchesWithAlgorithm(User user, String location) {
        DatingFilters filters = new DatingFilters();
        filters.setLocation(location);
        return getPotentialMatchesWithAlgorithm(user, filters);
    }

    public List<DatingProfile> getPotentialMatchesWithAlgorithm(User user, DatingFilters filters) {
        DatingProfile userProfile = getDatingProfileByUser(user);

        if (userProfile == null || !user.isEligibleForDating()) {
            return new ArrayList<>();
        }

        // Get base pool of eligible profiles
        List<DatingProfile> basePool = getBaseEligibleProfiles(user, userProfile, filters);

        // Apply the card stack algorithm
        return applyCardStackAlgorithm(user, userProfile, basePool);
    }

    // ==================== SWIPE ACTIONS ====================

    public Match swipeUser(User swiper, User target, SwipeDirection direction) {
        // Check if already swiped - but allow re-swiping if target liked swiper
        Optional<Swipe> existingSwipe = swipeRepository.findBySwiperAndTarget(swiper, target);

        if (existingSwipe.isPresent()) {
            // Allow re-swiping only if target has liked the swiper since the original swipe
            Optional<Swipe> targetLikedSwiper = swipeRepository.findBySwiperAndTarget(target, swiper);

            if (targetLikedSwiper.isEmpty() ||
                    targetLikedSwiper.get().getSwipedAt().isBefore(existingSwipe.get().getSwipedAt())) {
                throw new RuntimeException("Already swiped on this user");
            }

            // Update existing swipe instead of creating new one
            Swipe swipe = existingSwipe.get();
            swipe.setDirection(direction);
            swipe.setSwipedAt(LocalDateTime.now());
            swipeRepository.save(swipe);
        } else {
            // Create new swipe
            Swipe swipe = new Swipe();
            swipe.setSwiper(swiper);
            swipe.setTarget(target);
            swipe.setDirection(direction);
            swipe.setSwipedAt(LocalDateTime.now());
            swipeRepository.save(swipe);
        }

        // Check for match if it was a LIKE or SUPER_LIKE
        if (direction == SwipeDirection.LIKE || direction == SwipeDirection.SUPER_LIKE) {
            Optional<Swipe> reciprocalSwipe = swipeRepository.findBySwiperAndTarget(target, swiper);
            if (reciprocalSwipe.isPresent() &&
                    (reciprocalSwipe.get().getDirection() == SwipeDirection.LIKE ||
                            reciprocalSwipe.get().getDirection() == SwipeDirection.SUPER_LIKE)) {

                // Check if match already exists
                List<Match> existingMatches = matchRepository.findActiveMatchesBetweenUsers(swiper.getId(), target.getId());
                if (!existingMatches.isEmpty()) {
                    return existingMatches.get(0);
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

        return null;
    }

    public Match likeUserBack(User swiper, User target) {
        // First, verify that the target user has actually liked the swiper
        Optional<Swipe> targetLikedSwiper = swipeRepository.findBySwiperAndTarget(target, swiper);

        if (targetLikedSwiper.isEmpty() ||
                (targetLikedSwiper.get().getDirection() != SwipeDirection.LIKE &&
                        targetLikedSwiper.get().getDirection() != SwipeDirection.SUPER_LIKE)) {
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
        List<Match> existingMatches = matchRepository.findActiveMatchesBetweenUsers(swiper.getId(), target.getId());
        if (!existingMatches.isEmpty()) {
            return existingMatches.get(0);
        }

        // Create new match
        Match match = new Match();
        match.setUser1(swiper);
        match.setUser2(target);
        match.setMatchedAt(LocalDateTime.now());
        match.setIsActive(true);
        return matchRepository.save(match);
    }

    // ==================== MATCHES ====================

    public List<Match> getUserMatches(User user) {
        return matchRepository.findActiveMatchesForUser(user);
    }

    public void markMatchAsSeen(Long matchId, User user) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        // Verify user is part of this match
        if (!match.getUser1().getId().equals(user.getId()) &&
                !match.getUser2().getId().equals(user.getId())) {
            throw new RuntimeException("User is not part of this match");
        }

        // Could add "seenByUser1" and "seenByUser2" fields to Match entity in the future
        // For now, this is a placeholder for tracking seen status
    }

    public boolean areUsersMatched(User user1, User user2) {
        List<Match> matches = matchRepository.findActiveMatchesBetweenUsers(user1.getId(), user2.getId());
        return !matches.isEmpty();
    }

    // ==================== PREMIUM FEATURES ====================

    public List<DatingProfile> getWhoLikedMe(User user) {
        Subscription subscription = subscriptionService.getCurrentUserSubscription();

        if (subscription.getTier() == SubscriptionTier.FREE) {
            throw new RuntimeException("Upgrade to see who liked you");
        }

        // Get all users who swiped LIKE or SUPER_LIKE on current user
        List<Swipe> likesReceived = swipeRepository.findLikesReceivedByUser(user);

        // Convert to profiles and filter out users who are already matched
        List<DatingProfile> profiles = likesReceived.stream()
                .map(swipe -> getDatingProfileByUser(swipe.getSwiper()))
                .filter(Objects::nonNull)
                .filter(profile -> !areUsersMatched(user, profile.getUser()))
                .collect(Collectors.toList());

        logger.debug("Likes before filtering: {}", likesReceived.size());
        logger.debug("Likes after filtering matched users: {}", profiles.size());

        // ESSENTIAL tier: only show last 5 likes
        if (subscription.getTier() == SubscriptionTier.ESSENTIAL) {
            return profiles.stream()
                    .limit(5)
                    .collect(Collectors.toList());
        }

        // PREMIUM+ tiers: show all likes
        return profiles;
    }

    // ==================== ALGORITHM METHODS ====================

    public void updateEloScores(User swiper, User target, SwipeDirection direction) {
        DatingProfile swiperProfile = getDatingProfileByUser(swiper);
        DatingProfile targetProfile = getDatingProfileByUser(target);

        if (swiperProfile == null || targetProfile == null) return;

        int swiperElo = swiperProfile.getEloScore();
        int targetElo = targetProfile.getEloScore();

        // Elo rating system
        double expectedScore = 1.0 / (1.0 + Math.pow(10.0, (targetElo - swiperElo) / 400.0));

        int K = 32; // K-factor
        double actualScore = (direction == SwipeDirection.LIKE || direction == SwipeDirection.SUPER_LIKE) ? 1.0 : 0.0;

        // Update target's Elo (they "played" against swiper's judgment)
        int newTargetElo = (int) Math.round(targetElo + K * (actualScore - expectedScore));
        targetProfile.setEloScore(Math.max(100, Math.min(3000, newTargetElo))); // Clamp between 100-3000

        // Update statistics
        targetProfile.setTotalSwipesReceived(targetProfile.getTotalSwipesReceived() + 1);
        if (direction == SwipeDirection.LIKE || direction == SwipeDirection.SUPER_LIKE) {
            targetProfile.setTotalLikesReceived(targetProfile.getTotalLikesReceived() + 1);
        }

        // Mark profile as no longer fresh after some swipes
        if (targetProfile.getTotalSwipesReceived() > 50) {
            targetProfile.setIsFreshProfile(false);
        }

        datingProfileRepository.save(targetProfile);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private List<DatingProfile> getBaseEligibleProfiles(User user, DatingProfile userProfile, DatingFilters filters) {
        String genderPreferenceStr = userProfile.getGenderPreference() != null ?
                userProfile.getGenderPreference().name() : "EVERYONE";

        String searchLocation = filters.getLocation() != null ? filters.getLocation() : userProfile.getLocation();

        if (filters.hasFilters()) {
            return datingProfileRepository.findEligibleProfilesForCardStackWithFilters(
                    user.getId(),
                    genderPreferenceStr,
                    userProfile.getMinAge(),
                    userProfile.getMaxAge(),
                    searchLocation,
                    filters.getLifestyle(),
                    filters.getReligion(),
                    filters.getRelationshipType(),
                    filters.getDrinking(),
                    filters.getSmoking(),
                    filters.getHasChildren(),
                    filters.getWantChildren()
            );
        } else {
            return datingProfileRepository.findEligibleProfilesForCardStack(
                    user.getId(),
                    genderPreferenceStr,
                    userProfile.getMinAge(),
                    userProfile.getMaxAge()
            );
        }
    }

    private List<DatingProfile> applyCardStackAlgorithm(User user, DatingProfile userProfile, List<DatingProfile> basePool) {
        List<DatingProfile> orderedStack = new ArrayList<>();

        // 1. SUPER LIKES FIRST (highest priority)
        List<DatingProfile> superLikes = basePool.stream()
                .filter(profile -> hasUserSuperLikedMe(profile.getUser(), user))
                .sorted((a, b) -> getMostRecentSuperLikeDate(b.getUser(), user)
                        .compareTo(getMostRecentSuperLikeDate(a.getUser(), user)))
                .collect(Collectors.toList());
        orderedStack.addAll(superLikes);

        // 2. RECENT LIKES (for premium users)
        if (subscriptionService.getCurrentUserSubscription().getTier().ordinal() >= SubscriptionTier.PREMIUM.ordinal()) {
            List<DatingProfile> recentLikes = basePool.stream()
                    .filter(profile -> !superLikes.contains(profile))
                    .filter(profile -> hasUserLikedMeRecently(profile.getUser(), user))
                    .sorted((a, b) -> getMostRecentLikeDate(b.getUser(), user)
                            .compareTo(getMostRecentLikeDate(a.getUser(), user)))
                    .collect(Collectors.toList());
            orderedStack.addAll(recentLikes);
        }

        // 3. BOOSTED PROFILES
        List<DatingProfile> boostedProfiles = basePool.stream()
                .filter(profile -> !orderedStack.contains(profile))
                .filter(profile -> profile.getProfileBoostUntil() != null &&
                        profile.getProfileBoostUntil().isAfter(LocalDateTime.now()))
                .sorted((a, b) -> b.getProfileBoostUntil().compareTo(a.getProfileBoostUntil()))
                .collect(Collectors.toList());
        orderedStack.addAll(boostedProfiles);

        // 4. PREMIUM/VIP USERS (subscription priority)
        List<DatingProfile> premiumUsers = basePool.stream()
                .filter(profile -> !orderedStack.contains(profile))
                .filter(profile -> getUserSubscriptionTier(profile.getUser()).ordinal() >= SubscriptionTier.PREMIUM.ordinal())
                .sorted(this::compareBySubscriptionAndActivity)
                .collect(Collectors.toList());
        orderedStack.addAll(premiumUsers);

        // 5. FRESH PROFILES (new users get boost)
        List<DatingProfile> freshProfiles = basePool.stream()
                .filter(profile -> !orderedStack.contains(profile))
                .filter(profile -> Boolean.TRUE.equals(profile.getIsFreshProfile()))
                .sorted((a, b) -> b.getUser().getCreatedAt().compareTo(a.getUser().getCreatedAt()))
                .collect(Collectors.toList());
        orderedStack.addAll(freshProfiles);

        // 6. ELO-BASED MATCHING (similar attractiveness scores)
        List<DatingProfile> remainingProfiles = basePool.stream()
                .filter(profile -> !orderedStack.contains(profile))
                .collect(Collectors.toList());

        List<DatingProfile> eloMatched = sortByEloCompatibility(userProfile, remainingProfiles);
        orderedStack.addAll(eloMatched);

        // Limit results and add some randomization to prevent staleness
        return addDiversityAndLimit(orderedStack, 50);
    }

    private List<DatingProfile> sortByEloCompatibility(DatingProfile userProfile, List<DatingProfile> profiles) {
        int userElo = userProfile.getEloScore();

        return profiles.stream()
                .sorted((a, b) -> {
                    // Primary: Elo proximity (prefer similar scores)
                    int eloA = Math.abs(a.getEloScore() - userElo);
                    int eloB = Math.abs(b.getEloScore() - userElo);
                    int eloComparison = Integer.compare(eloA, eloB);

                    if (eloComparison != 0) return eloComparison;

                    // Secondary: Recent activity
                    LocalDateTime lastActiveA = a.getUser().getLastActive();
                    LocalDateTime lastActiveB = b.getUser().getLastActive();

                    if (lastActiveA != null && lastActiveB != null) {
                        return lastActiveB.compareTo(lastActiveA);
                    }

                    // Tertiary: Higher Elo scores (more attractive profiles)
                    return Integer.compare(b.getEloScore(), a.getEloScore());
                })
                .collect(Collectors.toList());
    }

    private List<DatingProfile> addDiversityAndLimit(List<DatingProfile> orderedStack, int limit) {
        // Take first portion as-is to maintain priority
        int priorityCount = Math.min(orderedStack.size(), limit / 3);
        List<DatingProfile> result = new ArrayList<>(orderedStack.subList(0, priorityCount));

        // Add diversity to the remaining slots
        List<DatingProfile> remaining = orderedStack.subList(priorityCount, orderedStack.size());
        Collections.shuffle(remaining); // Add some randomness

        int remainingSlots = limit - priorityCount;
        result.addAll(remaining.subList(0, Math.min(remaining.size(), remainingSlots)));

        return result;
    }

    private boolean hasUserSuperLikedMe(User potentialLiker, User currentUser) {
        Optional<Swipe> swipe = swipeRepository.findBySwiperAndTarget(potentialLiker, currentUser);
        return swipe.isPresent() && swipe.get().getDirection() == SwipeDirection.SUPER_LIKE;
    }

    private boolean hasUserLikedMeRecently(User potentialLiker, User currentUser) {
        Optional<Swipe> swipe = swipeRepository.findBySwiperAndTarget(potentialLiker, currentUser);
        if (swipe.isEmpty() || swipe.get().getDirection() != SwipeDirection.LIKE) {
            return false;
        }
        // Consider "recent" as within last 24 hours
        return swipe.get().getSwipedAt().isAfter(LocalDateTime.now().minusHours(24));
    }

    private LocalDateTime getMostRecentSuperLikeDate(User potentialLiker, User currentUser) {
        Optional<Swipe> swipe = swipeRepository.findBySwiperAndTarget(potentialLiker, currentUser);
        if (swipe.isPresent() && swipe.get().getDirection() == SwipeDirection.SUPER_LIKE) {
            return swipe.get().getSwipedAt();
        }
        return LocalDateTime.MIN; // Return very old date if no super like
    }

    private LocalDateTime getMostRecentLikeDate(User potentialLiker, User currentUser) {
        Optional<Swipe> swipe = swipeRepository.findBySwiperAndTarget(potentialLiker, currentUser);
        if (swipe.isPresent() && swipe.get().getDirection() == SwipeDirection.LIKE) {
            return swipe.get().getSwipedAt();
        }
        return LocalDateTime.MIN; // Return very old date if no like
    }

    private SubscriptionTier getUserSubscriptionTier(User user) {
        try {
            Subscription subscription = subscriptionService.getOrCreateSubscription(user);
            return subscription.getTier();
        } catch (Exception e) {
            logger.warn("Error getting subscription for user {}: {}", user.getId(), e.getMessage());
            return SubscriptionTier.FREE; // Default to free if error
        }
    }

    private int compareBySubscriptionAndActivity(DatingProfile a, DatingProfile b) {
        // Primary: Subscription tier (higher is better)
        SubscriptionTier tierA = getUserSubscriptionTier(a.getUser());
        SubscriptionTier tierB = getUserSubscriptionTier(b.getUser());
        int tierComparison = Integer.compare(tierB.ordinal(), tierA.ordinal());

        if (tierComparison != 0) return tierComparison;

        // Secondary: Recent activity
        LocalDateTime lastActiveA = a.getUser().getLastActive();
        LocalDateTime lastActiveB = b.getUser().getLastActive();

        if (lastActiveA != null && lastActiveB != null) {
            return lastActiveB.compareTo(lastActiveA);
        } else if (lastActiveA != null) {
            return -1; // A is more recent
        } else if (lastActiveB != null) {
            return 1; // B is more recent
        }

        return 0; // Both null, equal
    }
}
