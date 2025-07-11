package com.jgy36.PoliticalApp.dto;

import com.jgy36.PoliticalApp.entity.SubscriptionStatus;
import com.jgy36.PoliticalApp.entity.SubscriptionTier;

import java.time.LocalDateTime;

public class SubscriptionDTO {
    private Long id;
    private SubscriptionTier tier;
    private SubscriptionStatus status;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime trialEnd;
    private Boolean cancelAtPeriodEnd;
    private String displayName;
    private Double monthlyPrice;
    private String description;

    // Usage data
    private Integer dailySwipesUsed;
    private Integer dailySuperLikesUsed;
    private Integer monthlyBoostsUsed;

    // Feature limits
    private Integer dailySwipeLimit;
    private Integer dailySuperLikeLimit;
    private Integer monthlyBoostLimit;

    // Constructors
    public SubscriptionDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SubscriptionTier getTier() { return tier; }
    public void setTier(SubscriptionTier tier) { this.tier = tier; }

    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }

    public LocalDateTime getCurrentPeriodStart() { return currentPeriodStart; }
    public void setCurrentPeriodStart(LocalDateTime currentPeriodStart) { this.currentPeriodStart = currentPeriodStart; }

    public LocalDateTime getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(LocalDateTime currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }

    public LocalDateTime getTrialEnd() { return trialEnd; }
    public void setTrialEnd(LocalDateTime trialEnd) { this.trialEnd = trialEnd; }

    public Boolean getCancelAtPeriodEnd() { return cancelAtPeriodEnd; }
    public void setCancelAtPeriodEnd(Boolean cancelAtPeriodEnd) { this.cancelAtPeriodEnd = cancelAtPeriodEnd; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Double getMonthlyPrice() { return monthlyPrice; }
    public void setMonthlyPrice(Double monthlyPrice) { this.monthlyPrice = monthlyPrice; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDailySwipesUsed() { return dailySwipesUsed; }
    public void setDailySwipesUsed(Integer dailySwipesUsed) { this.dailySwipesUsed = dailySwipesUsed; }

    public Integer getDailySuperLikesUsed() { return dailySuperLikesUsed; }
    public void setDailySuperLikesUsed(Integer dailySuperLikesUsed) { this.dailySuperLikesUsed = dailySuperLikesUsed; }

    public Integer getMonthlyBoostsUsed() { return monthlyBoostsUsed; }
    public void setMonthlyBoostsUsed(Integer monthlyBoostsUsed) { this.monthlyBoostsUsed = monthlyBoostsUsed; }

    public Integer getDailySwipeLimit() { return dailySwipeLimit; }
    public void setDailySwipeLimit(Integer dailySwipeLimit) { this.dailySwipeLimit = dailySwipeLimit; }

    public Integer getDailySuperLikeLimit() { return dailySuperLikeLimit; }
    public void setDailySuperLikeLimit(Integer dailySuperLikeLimit) { this.dailySuperLikeLimit = dailySuperLikeLimit; }

    public Integer getMonthlyBoostLimit() { return monthlyBoostLimit; }
    public void setMonthlyBoostLimit(Integer monthlyBoostLimit) { this.monthlyBoostLimit = monthlyBoostLimit; }
}
