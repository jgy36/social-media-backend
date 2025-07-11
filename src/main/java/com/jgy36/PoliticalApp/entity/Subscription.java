package com.jgy36.PoliticalApp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionTier tier = SubscriptionTier.FREE;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "stripe_price_id")
    private String stripePriceId;

    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Column(name = "trial_end")
    private LocalDateTime trialEnd;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "cancel_at_period_end")
    private Boolean cancelAtPeriodEnd = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Usage tracking for rate limiting
    @Column(name = "daily_swipes_used")
    private Integer dailySwipesUsed = 0;

    @Column(name = "daily_super_likes_used")
    private Integer dailySuperLikesUsed = 0;

    @Column(name = "monthly_boosts_used")
    private Integer monthlyBoostsUsed = 0;

    @Column(name = "last_reset_date")
    private LocalDateTime lastResetDate = LocalDateTime.now();

    // Constructors
    public Subscription() {}

    public Subscription(User user) {
        this.user = user;
        this.tier = SubscriptionTier.FREE;
        this.status = SubscriptionStatus.ACTIVE;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public SubscriptionTier getTier() { return tier; }
    public void setTier(SubscriptionTier tier) { this.tier = tier; }

    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }

    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String stripeSubscriptionId) { this.stripeSubscriptionId = stripeSubscriptionId; }

    public String getStripePriceId() { return stripePriceId; }
    public void setStripePriceId(String stripePriceId) { this.stripePriceId = stripePriceId; }

    public LocalDateTime getCurrentPeriodStart() { return currentPeriodStart; }
    public void setCurrentPeriodStart(LocalDateTime currentPeriodStart) { this.currentPeriodStart = currentPeriodStart; }

    public LocalDateTime getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(LocalDateTime currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }

    public LocalDateTime getTrialEnd() { return trialEnd; }
    public void setTrialEnd(LocalDateTime trialEnd) { this.trialEnd = trialEnd; }

    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }

    public Boolean getCancelAtPeriodEnd() { return cancelAtPeriodEnd; }
    public void setCancelAtPeriodEnd(Boolean cancelAtPeriodEnd) { this.cancelAtPeriodEnd = cancelAtPeriodEnd; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getDailySwipesUsed() { return dailySwipesUsed; }
    public void setDailySwipesUsed(Integer dailySwipesUsed) { this.dailySwipesUsed = dailySwipesUsed; }

    public Integer getDailySuperLikesUsed() { return dailySuperLikesUsed; }
    public void setDailySuperLikesUsed(Integer dailySuperLikesUsed) { this.dailySuperLikesUsed = dailySuperLikesUsed; }

    public Integer getMonthlyBoostsUsed() { return monthlyBoostsUsed; }
    public void setMonthlyBoostsUsed(Integer monthlyBoostsUsed) { this.monthlyBoostsUsed = monthlyBoostsUsed; }

    public LocalDateTime getLastResetDate() { return lastResetDate; }
    public void setLastResetDate(LocalDateTime lastResetDate) { this.lastResetDate = lastResetDate; }

    // Helper methods
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }

    public boolean isOnTrial() {
        return trialEnd != null && LocalDateTime.now().isBefore(trialEnd);
    }

    public boolean hasFeature(String feature) {
        return tier.ordinal() >= getRequiredTierForFeature(feature).ordinal();
    }

    private SubscriptionTier getRequiredTierForFeature(String feature) {
        switch (feature) {
            case "unlimited_swipes":
            case "undo_swipe":
            case "ad_free":
                return SubscriptionTier.ESSENTIAL;
            case "unlimited_super_likes":
            case "see_all_likes":
            case "passport_mode":
            case "advanced_filters":
                return SubscriptionTier.PREMIUM;
            case "message_before_matching":
            case "vip_badge":
            case "priority_support":
                return SubscriptionTier.VIP;
            default:
                return SubscriptionTier.FREE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
