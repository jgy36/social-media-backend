package com.jgy36.PoliticalApp.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "communities")
public class Community {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String slug; // URL-friendly identifier

    @Column(unique = true, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "creator_id")
    @JsonIgnoreProperties({"password", "email", "verificationToken", "following", "savedPosts"})
    private User creator;

    @ManyToMany
    @JoinTable(
            name = "community_members",
            joinColumns = @JoinColumn(name = "community_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnoreProperties({"password", "email", "verificationToken", "following", "savedPosts"})
    private Set<User> members = new HashSet<>();

    @OneToMany(mappedBy = "community", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("community")
    private Set<Post> posts = new HashSet<>();

    private String color;
    private String bannerImage;

    // Keep only this approach for storing rules
    @ElementCollection
    @CollectionTable(name = "community_rules", joinColumns = @JoinColumn(name = "community_id"))
    @Column(name = "rule")
    private Set<String> rules = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "community_moderators",
            joinColumns = @JoinColumn(name = "community_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnoreProperties({"password", "email", "verificationToken", "following", "savedPosts"})
    private Set<User> moderators = new HashSet<>();

    // Default constructor required by JPA
    public Community() {
    }

    // Constructor with required fields that's being used in CommunityService
    public Community(String name, String slug, String description, User creator) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.creator = creator;
        this.createdAt = LocalDateTime.now();

        // Add creator as a member and moderator
        this.members.add(creator);
        this.moderators.add(creator);
    }

    // Helper methods - REMOVED the lifecycle hooks and JSON serialization methods

    // Helper methods
    public int getMemberCount() {
        return members.size();
    }

    public void addMember(User user) {
        members.add(user);
    }

    public void removeMember(User user) {
        members.remove(user);
    }

    public boolean isMember(User user) {
        return members.contains(user);
    }

    public void addModerator(User user) {
        moderators.add(user);
        // Ensure moderator is also a member
        addMember(user);
    }

    public void removeModerator(User user) {
        moderators.remove(user);
    }

    public boolean isModerator(User user) {
        return moderators.contains(user);
    }
}
