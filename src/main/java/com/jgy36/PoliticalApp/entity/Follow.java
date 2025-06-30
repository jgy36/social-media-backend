package com.jgy36.PoliticalApp.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "follows")
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ Getters & Setters
    @Getter
    @ManyToOne
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @Getter
    @ManyToOne
    @JoinColumn(name = "following_id", nullable = false)
    private User following;

    public Follow() {
    }

    public Follow(User follower, User following) {
        this.follower = follower;
        this.following = following;
    }

}
