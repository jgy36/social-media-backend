package com.jgy36.PoliticalApp.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "swipe")
public class Swipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "swiper_id")
    private User swiper;

    @ManyToOne
    @JoinColumn(name = "target_id")
    private User target;

    @Enumerated(EnumType.STRING)
    private SwipeDirection direction;

    @Column(name = "swiped_at")
    private LocalDateTime swipedAt;

    // Default constructor
    public Swipe() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getSwiper() { return swiper; }
    public void setSwiper(User swiper) { this.swiper = swiper; }

    public User getTarget() { return target; }
    public void setTarget(User target) { this.target = target; }

    public SwipeDirection getDirection() { return direction; }
    public void setDirection(SwipeDirection direction) { this.direction = direction; }

    public LocalDateTime getSwipedAt() { return swipedAt; }
    public void setSwipedAt(LocalDateTime swipedAt) { this.swipedAt = swipedAt; }
}
