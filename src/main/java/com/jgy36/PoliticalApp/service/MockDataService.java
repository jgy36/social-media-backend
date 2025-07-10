package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.entity.*;
import com.jgy36.PoliticalApp.repository.DatingProfileRepository;
import com.jgy36.PoliticalApp.repository.MatchRepository;
import com.jgy36.PoliticalApp.repository.SwipeRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    @Autowired
    private SwipeRepository swipeRepository;

    private Random random = new Random();

    // Sample data arrays
    private String[] firstNames = {
            "Alex", "Jordan", "Taylor", "Morgan", "Casey", "Riley", "Avery", "Quinn",
            "Blake", "Cameron", "Drew", "Emery", "Finley", "Harper", "Jamie", "Kendall",
            "Logan", "Parker", "Sage", "River", "Sam", "Sky", "Phoenix", "Eden"
    };

    private String[] femaleNames = {
            "Emma", "Olivia", "Ava", "Isabella", "Sophia", "Charlotte", "Mia", "Amelia",
            "Harper", "Evelyn", "Abigail", "Emily", "Elizabeth", "Mila", "Ella", "Avery",
            "Sofia", "Camila", "Aria", "Scarlett", "Victoria", "Madison", "Luna", "Grace"
    };

    private String[] maleNames = {
            "Liam", "Noah", "Oliver", "Elijah", "William", "James", "Benjamin", "Lucas",
            "Henry", "Alexander", "Mason", "Michael", "Ethan", "Daniel", "Jacob", "Logan",
            "Jackson", "Levi", "Sebastian", "Mateo", "Jack", "Owen", "Theodore", "Aiden"
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

        // Generate a mix of genders - ensure we have enough women for testing
        int womenCount = Math.max(5, count / 2); // At least 5 women, or half the total
        int menCount = count / 3;
        int otherCount = count - womenCount - menCount;

        System.out.println("📊 Gender distribution: " + womenCount + " women, " + menCount + " men, " + otherCount + " other/non-binary");

        int userIndex = 0;

        // Create women first
        for (int i = 0; i < womenCount; i++) {
            try {
                User user = createMockUser(userIndex++, Gender.WOMAN);
                User savedUser = userRepository.save(user);
                DatingProfile profile = createMockDatingProfile(savedUser, Gender.WOMAN);
                datingProfileRepository.save(profile);
                System.out.println("✅ Created woman: " + savedUser.getUsername());
            } catch (Exception e) {
                System.err.println("❌ Failed to create mock woman " + i + ": " + e.getMessage());
            }
        }

        // Create men
        for (int i = 0; i < menCount; i++) {
            try {
                User user = createMockUser(userIndex++, Gender.MAN);
                User savedUser = userRepository.save(user);
                DatingProfile profile = createMockDatingProfile(savedUser, Gender.MAN);
                datingProfileRepository.save(profile);
                System.out.println("✅ Created man: " + savedUser.getUsername());
            } catch (Exception e) {
                System.err.println("❌ Failed to create mock man " + i + ": " + e.getMessage());
            }
        }

        // Create other genders
        for (int i = 0; i < otherCount; i++) {
            try {
                Gender randomGender = random.nextBoolean() ? Gender.NON_BINARY : Gender.OTHER;
                User user = createMockUser(userIndex++, randomGender);
                User savedUser = userRepository.save(user);
                DatingProfile profile = createMockDatingProfile(savedUser, randomGender);
                datingProfileRepository.save(profile);
                System.out.println("✅ Created " + randomGender.getDisplayName().toLowerCase() + ": " + savedUser.getUsername());
            } catch (Exception e) {
                System.err.println("❌ Failed to create mock user " + i + ": " + e.getMessage());
            }
        }

        System.out.println("🎉 Mock data generation complete!");
    }

    private User createMockUser(int index, Gender gender) {
        String firstName;

        // Choose name based on gender
        switch (gender) {
            case WOMAN:
                firstName = femaleNames[random.nextInt(femaleNames.length)];
                break;
            case MAN:
                firstName = maleNames[random.nextInt(maleNames.length)];
                break;
            default:
                firstName = firstNames[random.nextInt(firstNames.length)];
                break;
        }

        String username = firstName.toLowerCase() + "_" + (1000 + index);
        String email = username + "@mockdating.app";

        // Generate age between 22-41
        int age = 22 + random.nextInt(20);

        // Calculate birth date based on age
        LocalDate dateOfBirth = LocalDate.now().minusYears(age).minusDays(random.nextInt(365));

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

        // ✅ SET THESE CRITICAL FIELDS FOR DATING ELIGIBILITY
        user.setDateOfBirth(dateOfBirth);
        user.setAgeConfirmed(true);

        System.out.println("📅 Created user " + username + " with age " + age + " (born " + dateOfBirth + ")");

        return user;
    }

    private DatingProfile createMockDatingProfile(User user, Gender gender) {
        DatingProfile profile = new DatingProfile();
        profile.setUser(user);
        profile.setBio(bios[random.nextInt(bios.length)]);

        // Use the same age calculation as the user
        profile.setAge(user.getAge());

        profile.setLocation(locations[random.nextInt(locations.length)]);
        profile.setHeight(generateRandomHeight());
        profile.setJob(jobs[random.nextInt(jobs.length)]);
        profile.setReligion(generateRandomReligion());
        profile.setRelationshipType(generateRandomRelationshipType());
        profile.setLifestyle(generateRandomLifestyle());

        // Generate photos (using placeholder images)
        profile.setPhotos(generateMockPhotos(gender));

        profile.setIsActive(true);

        // ✅ SET THE SPECIFIC GENDER PASSED IN
        profile.setGender(gender);

        // Set gender preference (for variety in testing)
        profile.setGenderPreference(generateRandomGenderPreference());

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

    // ADD these new methods for generating random enums
    private GenderPreference generateRandomGenderPreference() {
        GenderPreference[] preferences = GenderPreference.values();
        return preferences[random.nextInt(preferences.length)];
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

    private List<String> generateMockPhotos(Gender gender) {
        // Different photo sets based on gender for more realistic testing
        String[][] femalePhotoSets = {
                {
                        "https://images.unsplash.com/photo-1494790108755-2616b05aa284?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=400&h=600&fit=crop"
                },
                {
                        "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1488161628813-04466f872be2?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&h=600&fit=crop"
                },
                {
                        "https://images.unsplash.com/photo-1552374196-c4e7ffc6e126?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1547425260-76bcadfb4f2c?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=400&h=600&fit=crop"
                }
        };

        String[][] malePhotoSets = {
                {
                        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=400&h=600&fit=crop"
                },
                {
                        "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1519345182560-3f2917c472ef?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1531891437562-4301cf35b7e4?w=400&h=600&fit=crop"
                },
                {
                        "https://images.unsplash.com/photo-1463453091185-61582044d556?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1507081323647-4d250478b919?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=400&h=600&fit=crop"
                }
        };

        String[][] neutralPhotoSets = {
                {
                        "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1544723795-3fb6469f5b39?w=400&h=600&fit=crop",
                        "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=400&h=600&fit=crop"
                }
        };

        String[][] selectedSets;
        switch (gender) {
            case WOMAN:
                selectedSets = femalePhotoSets;
                break;
            case MAN:
                selectedSets = malePhotoSets;
                break;
            default:
                selectedSets = neutralPhotoSets;
                break;
        }

        // Pick a random photo set
        String[] selectedSet = selectedSets[random.nextInt(selectedSets.length)];

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

    /**
     * ✅ UPDATED: Create test scenario where WOMEN have already liked the real user
     * When the real user likes them back, they'll get instant matches!
     */
    public void createTestMatches(String realUserEmail) {
        try {
            System.out.println("🎯 Creating test match scenarios...");

            // Find the real user
            User realUser = userRepository.findByEmail(realUserEmail)
                    .orElseThrow(() -> new RuntimeException("Real user not found: " + realUserEmail));

            // ✅ GET ONLY WOMEN who have dating profiles
            List<User> womenUsers = userRepository.findByEmailContaining("@mockdating.app")
                    .stream()
                    .filter(user -> {
                        // Check if user has a dating profile and is a woman
                        return datingProfileRepository.findByUser(user)
                                .map(profile -> profile.getGender() == Gender.WOMAN)
                                .orElse(false);
                    })
                    .limit(10) // Get more women to choose from
                    .collect(Collectors.toList());

            if (womenUsers.size() < 3) {
                System.out.println("❌ Need at least 3 women in mock data! Found: " + womenUsers.size());
                System.out.println("💡 Generate more mock users first with: generateMockUsers(20)");
                return;
            }

            System.out.println("👩 Found " + womenUsers.size() + " women for test scenarios");

            // ✅ CREATE LIKE SWIPES FROM 3 WOMEN TO REAL USER
            for (int i = 0; i < Math.min(3, womenUsers.size()); i++) {
                User womanUser = womenUsers.get(i);

                // Check if this swipe already exists
                if (swipeRepository.existsBySwiperAndTarget(womanUser, realUser)) {
                    System.out.println("⚠️ Swipe already exists from " + womanUser.getUsername() + " to " + realUser.getUsername());
                    continue;
                }

                // Create a LIKE swipe from woman to real user
                Swipe swipe = new Swipe();
                swipe.setSwiper(womanUser);
                swipe.setTarget(realUser);
                swipe.setDirection(SwipeDirection.LIKE);
                swipe.setSwipedAt(LocalDateTime.now().minusHours(random.nextInt(24))); // Random time in last 24 hours

                swipeRepository.save(swipe);

                System.out.println("✅ Created LIKE swipe: " + womanUser.getUsername() + " (WOMAN) → " + realUser.getUsername());
            }

            // ✅ GET SOME MEN for PASS swipes (so they won't show up in your feed)
            List<User> menUsers = userRepository.findByEmailContaining("@mockdating.app")
                    .stream()
                    .filter(user -> {
                        return datingProfileRepository.findByUser(user)
                                .map(profile -> profile.getGender() == Gender.MAN)
                                .orElse(false);
                    })
                    .limit(5)
                    .collect(Collectors.toList());

            // Create PASS swipes from some men (for realistic data)
            for (int i = 0; i < Math.min(3, menUsers.size()); i++) {
                User manUser = menUsers.get(i);

                if (swipeRepository.existsBySwiperAndTarget(manUser, realUser)) {
                    continue;
                }

                Swipe swipe = new Swipe();
                swipe.setSwiper(manUser);
                swipe.setTarget(realUser);
                swipe.setDirection(SwipeDirection.PASS);
                swipe.setSwipedAt(LocalDateTime.now().minusHours(random.nextInt(48)));

                swipeRepository.save(swipe);

                System.out.println("✅ Created PASS swipe: " + manUser.getUsername() + " (MAN) → " + realUser.getUsername());
            }

            System.out.println("🎉 Test match scenarios created!");
            System.out.println("💡 Now when you like the first 3 women, you'll get instant matches!");
            System.out.println("🔍 Since your preferences are set to WOMEN only, you should only see women in your swiping feed.");

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
     * ✅ UPDATED: Create a complete test scenario with proper gender filtering
     */
    public void createComprehensiveTestScenario(String realUserEmail) {
        try {
            clearTestSwipes(realUserEmail); // Start fresh
            createTestMatches(realUserEmail); // Create the like scenarios

            // Print summary
            List<User> allMockUsers = userRepository.findByEmailContaining("@mockdating.app");
            long womenCount = allMockUsers.stream()
                    .filter(user -> datingProfileRepository.findByUser(user)
                            .map(profile -> profile.getGender() == Gender.WOMAN)
                            .orElse(false))
                    .count();

            long menCount = allMockUsers.stream()
                    .filter(user -> datingProfileRepository.findByUser(user)
                            .map(profile -> profile.getGender() == Gender.MAN)
                            .orElse(false))
                    .count();

            System.out.println("🎯 Comprehensive test scenario created!");
            System.out.println("📊 Mock user gender breakdown:");
            System.out.println("   • Women: " + womenCount);
            System.out.println("   • Men: " + menCount);
            System.out.println("   • Other: " + (allMockUsers.size() - womenCount - menCount));
            System.out.println("📋 Test scenarios:");
            System.out.println("   • 3 WOMEN have LIKED you (swipe right for instant matches!)");
            System.out.println("   • 3 men have PASSED on you (won't appear in your feed)");
            System.out.println("   • All other women are fresh (no previous swipes)");
            System.out.println("🔍 Since your preference is WOMEN only, you should only see women while swiping!");

        } catch (Exception e) {
            System.err.println("❌ Failed to create comprehensive test scenario: " + e.getMessage());
        }
    }

    /**
     * ✅ NEW: Debug method to check gender distribution and eligibility
     */
    public void debugGenderDistribution() {
        try {
            List<User> allMockUsers = userRepository.findByEmailContaining("@mockdating.app");

            System.out.println("🔍 MOCK USER DEBUG REPORT");
            System.out.println("========================");
            System.out.println("Total mock users: " + allMockUsers.size());

            for (User user : allMockUsers) {
                DatingProfile profile = datingProfileRepository.findByUser(user).orElse(null);
                System.out.println(String.format("👤 %s | Age: %d | Gender: %s | Eligible: %s | Profile: %s",
                        user.getUsername(),
                        user.getAge(),
                        profile != null ? profile.getGender().getDisplayName() : "NO_PROFILE",
                        user.isEligibleForDating() ? "✅" : "❌",
                        profile != null && profile.getIsActive() ? "ACTIVE" : "INACTIVE"
                ));
            }

        } catch (Exception e) {
            System.err.println("❌ Debug failed: " + e.getMessage());
        }
    }
}
