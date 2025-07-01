package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.entity.*;
import com.jgy36.PoliticalApp.repository.DatingProfileRepository;
import com.jgy36.PoliticalApp.repository.MatchRepository;
import com.jgy36.PoliticalApp.repository.SwipeRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class MockDataService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatingProfileRepository datingProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MatchRepository matchRepository;

    private Random random = new Random();

    // Sample data arrays
    private String[] firstNames = {
            "Alex", "Jordan", "Taylor", "Morgan", "Casey", "Riley", "Avery", "Quinn",
            "Blake", "Cameron", "Drew", "Emery", "Finley", "Harper", "Jamie", "Kendall",
            "Logan", "Parker", "Sage", "River", "Sam", "Sky", "Phoenix", "Eden"
    };

    private String[] locations = {
            "New York, NY", "Los Angeles, CA", "Chicago, IL", "Houston, TX", "Phoenix, AZ",
            "Philadelphia, PA", "San Antonio, TX", "San Diego, CA", "Dallas, TX", "San Jose, CA",
            "Austin, TX", "Jacksonville, FL", "San Francisco, CA", "Indianapolis, IN", "Columbus, OH",
            "Fort Worth, TX", "Charlotte, NC", "Detroit, MI", "El Paso, TX", "Seattle, WA"
    };

    private String[] jobs = {
            "Software Engineer", "Teacher", "Doctor", "Lawyer", "Artist", "Nurse", "Marketing Manager",
            "Photographer", "Chef", "Personal Trainer", "Therapist", "Accountant", "Designer",
            "Writer", "Consultant", "Sales Representative", "Engineer", "Musician", "Student", "Entrepreneur"
    };

    private String[] bios = {
            "Love hiking and coffee shops ☕️ Looking for someone who enjoys deep conversations and outdoor adventures.",
            "Foodie, traveler, and dog lover 🐕 Let's explore the world together!",
            "Fitness enthusiast who also binge-watches Netflix. Balance is key 😄",
            "Artist by day, dreamer by night ✨ Seeking creative souls and genuine connections.",
            "Tech professional who loves weekend camping trips 🏕️ Swipe right for adventure stories!",
            "Bookworm and coffee addict ☕️ Always up for trying new restaurants and having meaningful conversations.",
            "Music lover and concert goer 🎵 Let's discover new artists together!",
            "Yoga instructor with a passion for mindful living 🧘‍♀️ Looking for someone who values growth.",
            "Amateur chef who loves cooking for others 👩‍🍳 Food is my love language!",
            "Adventure seeker and photography enthusiast 📸 Life's too short for boring dates!"
    };

    private String[] prompts = {
            "My perfect Sunday involves...",
            "I'm looking for someone who...",
            "The best way to my heart is...",
            "My biggest dream is...",
            "I can't live without...",
            "My hidden talent is...",
            "The last book I read was...",
            "My ideal vacation would be...",
            "I'm passionate about...",
            "My guilty pleasure is..."
    };

    public void generateMockUsers(int count) {
        System.out.println("🎭 Generating " + count + " mock users for dating...");

        for (int i = 0; i < count; i++) {
            try {
                // Create user
                User user = createMockUser(i);
                User savedUser = userRepository.save(user);

                // Create dating profile
                DatingProfile profile = createMockDatingProfile(savedUser);
                datingProfileRepository.save(profile);

                System.out.println("✅ Created user: " + savedUser.getUsername());
            } catch (Exception e) {
                System.err.println("❌ Failed to create mock user " + i + ": " + e.getMessage());
            }
        }

        System.out.println("🎉 Mock data generation complete!");
    }

    private User createMockUser(int index) {
        String firstName = firstNames[random.nextInt(firstNames.length)];
        String username = firstName.toLowerCase() + "_" + (1000 + index);
        String email = username + "@mockdating.app";

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123")); // All mock users have same password
        user.setDisplayName(firstName);
        user.setRole(Role.ROLE_USER);
        user.setVerified(true);
        user.setEmailVerified(true);
        user.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(365))); // Random join date within last year
        user.setDatingModeEnabled(true);
        user.setDatingProfileComplete(true);
        user.setLastActive(LocalDateTime.now().minusHours(random.nextInt(48))); // Active within last 48 hours

        return user;
    }

    private DatingProfile createMockDatingProfile(User user) {
        DatingProfile profile = new DatingProfile();
        profile.setUser(user);
        profile.setBio(bios[random.nextInt(bios.length)]);
        profile.setAge(22 + random.nextInt(20)); // Age between 22-41
        profile.setLocation(locations[random.nextInt(locations.length)]);
        profile.setHeight(generateRandomHeight());
        profile.setJob(jobs[random.nextInt(jobs.length)]);
        profile.setReligion(generateRandomReligion());
        profile.setRelationshipType(generateRandomRelationshipType());
        profile.setLifestyle(generateRandomLifestyle());

        // Generate photos (using placeholder images)
        profile.setPhotos(generateMockPhotos());

        profile.setIsActive(true);
        profile.setGenderPreference("Everyone"); // Simplified for testing
        profile.setMinAge(20);
        profile.setMaxAge(50);
        profile.setMaxDistance(25);

        // Generate prompts
        profile.setPrompts(generateMockPrompts());

        // Set vitals & vices
        profile.setHasChildren(generateRandomOption(new String[]{"No", "Yes"}));
        profile.setWantChildren(generateRandomOption(new String[]{"Yes", "No", "Maybe"}));
        profile.setDrinking(generateRandomOption(new String[]{"Never", "Sometimes", "Frequently"}));
        profile.setSmoking(generateRandomOption(new String[]{"No", "Sometimes", "Yes"}));
        profile.setDrugs(generateRandomOption(new String[]{"No", "Sometimes", "Yes"}));
        profile.setLookingFor(generateRandomLookingFor());

        // Generate interests and virtues
        profile.setInterests(generateMockInterests());
        profile.setVirtues(generateMockVirtues());

        return profile;
    }

    private String generateRandomHeight() {
        int feet = 5 + random.nextInt(2); // 5 or 6 feet
        int inches = random.nextInt(12); // 0-11 inches
        return feet + "'" + inches + "\"";
    }

    private String generateRandomReligion() {
        String[] religions = {"Christian", "Muslim", "Jewish", "Hindu", "Buddhist", "Atheist", "Agnostic", "Spiritual", "Other"};
        return religions[random.nextInt(religions.length)];
    }

    private String generateRandomRelationshipType() {
        String[] types = {"Long-term relationship", "Casual dating", "New friends", "Open to anything"};
        return types[random.nextInt(types.length)];
    }

    private String generateRandomLifestyle() {
        String[] lifestyles = {"Active", "Laid back", "Social butterfly", "Homebody", "Adventurous", "Career-focused"};
        return lifestyles[random.nextInt(lifestyles.length)];
    }

    private String generateRandomOption(String[] options) {
        return options[random.nextInt(options.length)];
    }

    private String generateRandomLookingFor() {
        String[] lookingFor = {
                "Someone genuine and kind",
                "A partner in crime for adventures",
                "Someone who shares my values",
                "A best friend and lover",
                "Someone to build a future with",
                "A person who makes me laugh",
                "Someone emotionally intelligent",
                "A fellow foodie and traveler"
        };
        return lookingFor[random.nextInt(lookingFor.length)];
    }

    // REPLACE the generateMockPhotos method in your MockDataService.java with this:

    private List<String> generateMockPhotos() {
        // Different photo sets for variety
        String[][] photoSets = {
                // Set 1
                {
                        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=400&h=600&fit=crop"
                },
                // Set 2
                {
                        "https://images.unsplash.com/photo-1494790108755-2616b05aa284?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=400&h=600&fit=crop"
                },
                // Set 3
                {
                        "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1488161628813-04466f872be2?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&h=600&fit=crop"
                },
                // Set 4
                {
                        "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1519345182560-3f2917c472ef?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1531891437562-4301cf35b7e4?w=400&h=600&fit=crop"
                },
                // Set 5
                {
                        "https://images.unsplash.com/photo-1463453091185-61582044d556?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1507081323647-4d250478b919?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=400&h=600&fit=crop"
                },
                // Set 6 - Women
                {
                        "https://images.unsplash.com/photo-1494790108755-2616b05aa284?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=400&h=600&fit=crop"
                },
                // Set 7 - Women
                {
                        "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1488161628813-04466f872be2?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&h=600&fit=crop"
                },
                // Set 8 - More variety
                {
                        "https://images.unsplash.com/photo-1552374196-c4e7ffc6e126?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1547425260-76bcadfb4f2c?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=400&h=600&fit=crop"
                }
        };

        // Pick a random photo set
        String[] selectedSet = photoSets[random.nextInt(photoSets.length)];

        // Return 1-3 photos from the selected set
        int numPhotos = 1 + random.nextInt(3); // 1-3 photos
        List<String> userPhotos = new ArrayList<>();

        for (int i = 0; i < numPhotos && i < selectedSet.length; i++) {
            userPhotos.add(selectedSet[i]);
        }

        return userPhotos;
    }

    private List<String> generateMockPrompts() {
        String[] promptAnswers = {
                "Hiking with my dog and a good book",
                "Someone who can make me laugh even on bad days",
                "Good food and genuine conversation",
                "To travel to every continent",
                "My morning coffee and favorite playlist",
                "I can solve a Rubik's cube in under 2 minutes",
                "The Seven Husbands of Evelyn Hugo",
                "Backpacking through Southeast Asia",
                "Environmental conservation and animal rights",
                "Watching The Office for the 100th time"
        };

        return Arrays.asList(
                "{\"question\":\"" + prompts[0] + "\",\"answer\":\"" + promptAnswers[0] + "\"}",
                "{\"question\":\"" + prompts[1] + "\",\"answer\":\"" + promptAnswers[1] + "\"}",
                "{\"question\":\"" + prompts[2] + "\",\"answer\":\"" + promptAnswers[2] + "\"}"
        );
    }

    private List<String> generateMockInterests() {
        String[] interests = {
                "Hiking", "Photography", "Cooking", "Travel", "Music", "Reading", "Fitness", "Art",
                "Dancing", "Gaming", "Movies", "Sports", "Yoga", "Writing", "Technology", "Nature"
        };

        int numInterests = 3 + random.nextInt(5); // 3-7 interests
        List<String> selectedInterests = Arrays.asList(interests).subList(0, Math.min(numInterests, interests.length));
        return selectedInterests.stream().map(interest -> "\"" + interest + "\"").collect(Collectors.toList());
    }

    private List<String> generateMockVirtues() {
        String[][] virtueOptions = {
                {"Communication", "Direct", "Thoughtful", "Expressive"},
                {"Humor", "Dry", "Silly", "Witty"},
                {"Energy", "High", "Calm", "Balanced"},
                {"Affection", "Physical touch", "Words", "Quality time"}
        };

        return Arrays.asList(
                "{\"category\":\"" + virtueOptions[0][0] + "\",\"value\":\"" + virtueOptions[0][1 + random.nextInt(3)] + "\"}",
                "{\"category\":\"" + virtueOptions[1][0] + "\",\"value\":\"" + virtueOptions[1][1 + random.nextInt(3)] + "\"}",
                "{\"category\":\"" + virtueOptions[2][0] + "\",\"value\":\"" + virtueOptions[2][1 + random.nextInt(3)] + "\"}"
        );
    }

    public void clearMockData() {
        System.out.println("🧹 Clearing existing mock data...");

        // Delete all dating profiles for mock users
        List<User> mockUsers = userRepository.findByEmailContaining("@mockdating.app");
        for (User user : mockUsers) {
            datingProfileRepository.findByUser(user).ifPresent(profile -> {
                datingProfileRepository.delete(profile);
            });
            userRepository.delete(user);
        }

        System.out.println("✅ Mock data cleared!");
    }
    // ADD this method to your MockDataService.java class

    @Autowired
    private SwipeRepository swipeRepository;

    /**
     * Create test scenario where some mock users have already liked the real user
     * When the real user likes them back, they'll get instant matches!
     */
    public void createTestMatches(String realUserEmail) {
        try {
            System.out.println("🎯 Creating test match scenarios...");

            // Find the real user
            User realUser = userRepository.findByEmail(realUserEmail)
                    .orElseThrow(() -> new RuntimeException("Real user not found: " + realUserEmail));

            // Get some mock users (first 5 mock users)
            List<User> mockUsers = userRepository.findByEmailContaining("@mockdating.app")
                    .stream()
                    .limit(5)
                    .collect(Collectors.toList());

            if (mockUsers.isEmpty()) {
                System.out.println("❌ No mock users found! Create mock users first.");
                return;
            }

            // Create swipes where mock users have already liked the real user
            for (int i = 0; i < Math.min(3, mockUsers.size()); i++) {
                User mockUser = mockUsers.get(i);

                // Check if this swipe already exists
                if (swipeRepository.existsBySwiperAndTarget(mockUser, realUser)) {
                    System.out.println("⚠️ Swipe already exists from " + mockUser.getUsername() + " to " + realUser.getUsername());
                    continue;
                }

                // Create a LIKE swipe from mock user to real user
                Swipe swipe = new Swipe();
                swipe.setSwiper(mockUser);
                swipe.setTarget(realUser);
                swipe.setDirection(SwipeDirection.LIKE);
                swipe.setSwipedAt(LocalDateTime.now().minusHours(random.nextInt(24))); // Random time in last 24 hours

                swipeRepository.save(swipe);

                System.out.println("✅ Created LIKE swipe: " + mockUser.getUsername() + " → " + realUser.getUsername());
            }

            // Also create some PASS swipes from other mock users (for realistic data)
            for (int i = 3; i < Math.min(6, mockUsers.size()); i++) {
                User mockUser = mockUsers.get(i);

                if (swipeRepository.existsBySwiperAndTarget(mockUser, realUser)) {
                    continue;
                }

                Swipe swipe = new Swipe();
                swipe.setSwiper(mockUser);
                swipe.setTarget(realUser);
                swipe.setDirection(SwipeDirection.PASS);
                swipe.setSwipedAt(LocalDateTime.now().minusHours(random.nextInt(48)));

                swipeRepository.save(swipe);

                System.out.println("✅ Created PASS swipe: " + mockUser.getUsername() + " → " + realUser.getUsername());
            }

            System.out.println("🎉 Test match scenarios created!");
            System.out.println("💡 Now when you like the first 3 mock users, you'll get instant matches!");

        } catch (Exception e) {
            System.err.println("❌ Failed to create test matches: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Clear existing test swipes for a user (useful for resetting test scenarios)
     */
    public void clearTestSwipes(String realUserEmail) {
        try {
            User realUser = userRepository.findByEmail(realUserEmail)
                    .orElseThrow(() -> new RuntimeException("Real user not found: " + realUserEmail));

            // Delete all swipes involving this user
            List<Swipe> swipesToDelete = swipeRepository.findAll().stream()
                    .filter(swipe -> swipe.getSwiper().getId().equals(realUser.getId()) ||
                            swipe.getTarget().getId().equals(realUser.getId()))
                    .collect(Collectors.toList());

            swipeRepository.deleteAll(swipesToDelete);

            // Also delete any existing matches
            List<Match> matchesToDelete = matchRepository.findAll().stream()
                    .filter(match -> match.getUser1().getId().equals(realUser.getId()) ||
                            match.getUser2().getId().equals(realUser.getId()))
                    .collect(Collectors.toList());

            matchRepository.deleteAll(matchesToDelete);

            System.out.println("✅ Cleared all test swipes and matches for " + realUser.getUsername());

        } catch (Exception e) {
            System.err.println("❌ Failed to clear test swipes: " + e.getMessage());
        }
    }

    /**
     * Create a complete test scenario with various swipe states
     */
    public void createComprehensiveTestScenario(String realUserEmail) {
        try {
            clearTestSwipes(realUserEmail); // Start fresh
            createTestMatches(realUserEmail); // Create the like scenarios

            System.out.println("🎯 Comprehensive test scenario created!");
            System.out.println("📋 Test scenarios:");
            System.out.println("   • 3 mock users have LIKED you (swipe right for instant matches!)");
            System.out.println("   • 3 mock users have PASSED on you (realistic data)");
            System.out.println("   • All other mock users are fresh (no previous swipes)");

        } catch (Exception e) {
            System.err.println("❌ Failed to create comprehensive test scenario: " + e.getMessage());
        }
    }
}
