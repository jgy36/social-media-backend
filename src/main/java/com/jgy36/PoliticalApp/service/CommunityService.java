package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.entity.Community;
import com.jgy36.PoliticalApp.entity.CommunityUserPreference;
import com.jgy36.PoliticalApp.entity.Post;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.exception.ResourceNotFoundException;
import com.jgy36.PoliticalApp.repository.CommunityRepository;
import com.jgy36.PoliticalApp.repository.CommunityUserPreferenceRepository;
import com.jgy36.PoliticalApp.repository.PostRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import com.jgy36.PoliticalApp.utils.SecurityUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CommunityUserPreferenceRepository communityUserPreferenceRepository;

    @Autowired
    public CommunityService(CommunityRepository communityRepository,
                            UserRepository userRepository,
                            PostRepository postRepository) {
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    // Initialize default political communities
    @PostConstruct
    @Transactional
    public void initDefaultCommunities() {
        // Only run if no communities exist
        if (communityRepository.count() == 0) {
            try {
                // Find admin user for initial creation
                User adminUser = userRepository.findByEmail("admin@example.com")
                        .orElseGet(() -> {
                            // If no admin exists, use the first user
                            List<User> users = userRepository.findAll();
                            if (!users.isEmpty()) {
                                return users.get(0);
                            }
                            return null;
                        });

                if (adminUser == null) {
                    System.out.println("Cannot initialize communities - no users exist yet");
                    return;
                }

                // Create default political communities
                createDefaultCommunity("democrat", "Democrat",
                        "Discussion forum for Democratic Party supporters and policy discussions.",
                        "#3b82f6", adminUser, // blue color
                        Arrays.asList(
                                "Be respectful of other members",
                                "No hate speech or personal attacks",
                                "Focus on policy discussion, not personal attacks",
                                "Cite sources for claims when possible"
                        ));

                createDefaultCommunity("republican", "Republican",
                        "Forum for Republican Party members and conservative policy discussions.",
                        "#ef4444", adminUser, // red color
                        Arrays.asList(
                                "Be respectful of other members",
                                "No hate speech or personal attacks",
                                "Focus on policy discussion, not personal attacks",
                                "Cite sources for claims when possible"
                        ));

                createDefaultCommunity("libertarian", "Libertarian",
                        "Discussion of Libertarian politics, policies, and philosophy.",
                        "#eab308", adminUser, // yellow color
                        Arrays.asList(
                                "Be respectful of other members",
                                "No hate speech or personal attacks",
                                "Focus on policy discussion, not personal attacks",
                                "Cite sources for claims when possible"
                        ));

                createDefaultCommunity("independent", "Independent",
                        "For politically independent voters and those seeking non-partisan discussion.",
                        "#a855f7", adminUser, // purple color
                        Arrays.asList(
                                "Be respectful of other members",
                                "No hate speech or personal attacks",
                                "Focus on policy discussion, not personal attacks",
                                "Cite sources for claims when possible"
                        ));

                createDefaultCommunity("conservative", "Conservative",
                        "Discussion of conservative principles, policies, and philosophy.",
                        "#b91c1c", adminUser, // dark red color
                        Arrays.asList(
                                "Be respectful of other members",
                                "No hate speech or personal attacks",
                                "Focus on policy discussion, not personal attacks",
                                "Cite sources for claims when possible"
                        ));

                createDefaultCommunity("socialist", "Socialist",
                        "Discussion of socialist politics, policies, and philosophy.",
                        "#b91c1c", adminUser, // dark red color
                        Arrays.asList(
                                "Be respectful of other members",
                                "No hate speech or personal attacks",
                                "Focus on policy discussion, not personal attacks",
                                "Cite sources for claims when possible"
                        ));

                System.out.println("Default communities created successfully");
            } catch (Exception e) {
                System.err.println("Error creating default communities: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Force initialization of default communities, even if some already exist
     * This method can be called from an admin endpoint to ensure communities are created
     */
    @Transactional
    public void forceInitDefaultCommunities() {
        try {
            // Find current user with email
            User adminUser = userRepository.findByEmail("bigben@example.com")
                    .orElseGet(() -> {
                        // If specific user not found, use any user
                        List<User> users = userRepository.findAll();
                        if (!users.isEmpty()) {
                            return users.get(0);
                        }
                        return null;
                    });

            if (adminUser == null) {
                System.out.println("Cannot initialize communities - no users exist yet");
                return;
            }

            // Create default political communities
            createDefaultCommunity("democrat", "Democrat",
                    "Discussion forum for Democratic Party supporters and policy discussions.",
                    "#3b82f6", adminUser, // blue color
                    Arrays.asList(
                            "Be respectful of other members",
                            "No hate speech or personal attacks",
                            "Focus on policy discussion, not personal attacks",
                            "Cite sources for claims when possible"
                    ));

            createDefaultCommunity("republican", "Republican",
                    "Forum for Republican Party members and conservative policy discussions.",
                    "#ef4444", adminUser, // red color
                    Arrays.asList(
                            "Be respectful of other members",
                            "No hate speech or personal attacks",
                            "Focus on policy discussion, not personal attacks",
                            "Cite sources for claims when possible"
                    ));

            createDefaultCommunity("libertarian", "Libertarian",
                    "Discussion of Libertarian politics, policies, and philosophy.",
                    "#eab308", adminUser, // yellow color
                    Arrays.asList(
                            "Be respectful of other members",
                            "No hate speech or personal attacks",
                            "Focus on policy discussion, not personal attacks",
                            "Cite sources for claims when possible"
                    ));

            createDefaultCommunity("independent", "Independent",
                    "For politically independent voters and those seeking non-partisan discussion.",
                    "#a855f7", adminUser, // purple color
                    Arrays.asList(
                            "Be respectful of other members",
                            "No hate speech or personal attacks",
                            "Focus on policy discussion, not personal attacks",
                            "Cite sources for claims when possible"
                    ));

            createDefaultCommunity("conservative", "Conservative",
                    "Discussion of conservative principles, policies, and philosophy.",
                    "#b91c1c", adminUser, // dark red color
                    Arrays.asList(
                            "Be respectful of other members",
                            "No hate speech or personal attacks",
                            "Focus on policy discussion, not personal attacks",
                            "Cite sources for claims when possible"
                    ));

            createDefaultCommunity("socialist", "Socialist",
                    "Discussion of socialist politics, policies, and philosophy.",
                    "#b91c1c", adminUser, // dark red color
                    Arrays.asList(
                            "Be respectful of other members",
                            "No hate speech or personal attacks",
                            "Focus on policy discussion, not personal attacks",
                            "Cite sources for claims when possible"
                    ));

            System.out.println("Default communities created successfully through force initialization");
        } catch (Exception e) {
            System.err.println("Error creating default communities: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error creating communities: " + e.getMessage(), e);
        }
    }

    private void createDefaultCommunity(String slug, String name, String description,
                                        String color, User creator, List<String> rules) {
        // Only create if it doesn't exist
        if (!communityRepository.existsBySlug(slug)) {
            Community community = new Community(name, slug, description, creator);
            community.setColor(color);
            community.getRules().addAll(rules);
            communityRepository.save(community);
            System.out.println("Created community: " + name);
        } else {
            System.out.println("Community already exists: " + name);
        }
    }

    // Get all communities
    public List<Community> getAllCommunities() {
        return communityRepository.findAll();
    }

    // Get community by ID - now returns by slug since we want to use slugs
    public Community getCommunityById(String slug) {
        return getCommunityBySlug(slug);
    }

    // Get community by slug
    public Community getCommunityBySlug(String slug) {
        return communityRepository.findBySlug(slug)
                .orElseThrow(() -> new NoSuchElementException("Community not found with slug: " + slug));
    }

    // Create a new community
    @Transactional
    public Community createCommunity(String slug, String name, String description, Set<String> rules, String color) {
        // Get the current user
        User currentUser = getCurrentUser();

        // Check if slug is already taken
        if (communityRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Community slug already exists. Please choose another one.");
        }

        // Create the community
        Community community = new Community(name, slug, description, currentUser);

        // Add rules and color if provided
        if (rules != null && !rules.isEmpty()) {
            community.getRules().addAll(rules);
        }

        if (color != null && !color.isEmpty()) {
            community.setColor(color);
        }

        return communityRepository.save(community);
    }

    // Join a community
    @Transactional
    public void joinCommunity(String slug) {
        User currentUser = getCurrentUser();
        Community community = getCommunityBySlug(slug);

        community.addMember(currentUser);
        communityRepository.save(community);
    }

    // Leave a community
    @Transactional
    public void leaveCommunity(String slug) {
        User currentUser = getCurrentUser();
        Community community = getCommunityBySlug(slug);

        community.removeMember(currentUser);
        communityRepository.save(community);
    }

    // Check if user is a member of a community
    public boolean isMember(String slug) {
        User currentUser = getCurrentUser();
        Community community = getCommunityBySlug(slug);

        return community.isMember(currentUser);
    }

    // Get communities user is a member of
    public List<Community> getUserCommunities() {
        User currentUser = getCurrentUser();
        return communityRepository.findCommunitiesByMember(currentUser);
    }

    // Search communities
    public List<Community> searchCommunities(String searchTerm) {
        return communityRepository.searchCommunities(searchTerm);
    }

    // Get trending communities
    public List<Community> getTrendingCommunities() {
        return communityRepository.findTrendingCommunities();
    }

    // Get popular communities
    public List<Community> getPopularCommunities() {
        return communityRepository.findPopularCommunities();
    }

    // Get posts from a community
    public List<Post> getCommunityPosts(String slug) {
        Community community = getCommunityBySlug(slug);
        return postRepository.findByCommunityOrderByCreatedAtDesc(community);
    }

    // Create a post in a community
    @Transactional
    public Post createCommunityPost(String slug, String content) {
        User currentUser = getCurrentUser();
        Community community = getCommunityBySlug(slug);

        // Check if user is a member
        if (!community.isMember(currentUser)) {
            throw new IllegalStateException("You must be a member of this community to post.");
        }

        Post post = new Post(content, currentUser);
        post.setCommunity(community);
        Post savedPost = postRepository.save(post);

        // Create notifications for users who have enabled notifications for this community
        createNotificationsForNewPost(community, currentUser, post);

        return savedPost;
    }

    // Add this new method
    private void createNotificationsForNewPost(Community community, User postAuthor, Post post) {
        // Get all user preferences for this community where notifications are enabled
        Iterable<CommunityUserPreference> preferences = communityUserPreferenceRepository.findAllByCommunity(community);

        for (CommunityUserPreference preference : preferences) {
            User user = preference.getUser();

            // Skip notification for the post author
            if (user.getId().equals(postAuthor.getId())) {
                continue;
            }

            // Only create notification if user has enabled notifications
            if (preference.isNotificationsEnabled()) {
                // Create notification with proper reference data
                notificationService.createPostNotification(user, postAuthor, post);
            }
        }
    }

    // Helper method to get the current authenticated user
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found with email: " + email));
    }

    /**
     * Toggle notification preferences for a community
     *
     * @param slug The community slug
     * @return The new notification state (true = on, false = off)
     */
    // In CommunityService.java - Fix to toggle method
    @Transactional
    public boolean toggleCommunityNotifications(String slug) {
        // Get the current user's principal name (might be email)
        String principalName = SecurityUtils.getCurrentUsername();
        if (principalName == null) {
            throw new RuntimeException("User not authenticated");
        }

        // Try to find by username first
        Optional<User> userOpt = userRepository.findByUsername(principalName);

        // If not found by username, try by email
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(principalName);
        }

        User currentUser = userOpt.orElseThrow(() ->
                new RuntimeException("User not found with identifier: " + principalName));

        // Get the community
        Community community = communityRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + slug));

        // Check if user is a member of the community
        if (!community.isMember(currentUser)) {
            throw new AccessDeniedException("You must be a member of this community to manage notifications");
        }

        // Get or create user preferences for this community
        CommunityUserPreference preference = communityUserPreferenceRepository
                .findByUserAndCommunity(currentUser, community)
                .orElseGet(() -> {
                    // Create new preference - SET TO FALSE BY DEFAULT
                    CommunityUserPreference newPref = new CommunityUserPreference(currentUser, community);
                    newPref.setNotificationsEnabled(false); // Set initial state to false (off)
                    return newPref;
                });

        // Get current state BEFORE toggling
        boolean currentState = preference.isNotificationsEnabled();
        System.out.println("Current notification state before toggle: " + currentState);

        // Toggle notification state
        boolean newState = !currentState;
        preference.setNotificationsEnabled(newState);

        // Save the preference
        communityUserPreferenceRepository.save(preference);

        System.out.println("New notification state after toggle: " + newState);

        return newState;
    }
}
