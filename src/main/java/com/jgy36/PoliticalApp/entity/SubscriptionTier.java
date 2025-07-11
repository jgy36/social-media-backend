package com.jgy36.PoliticalApp.entity;

public enum SubscriptionTier {
    FREE("Free", 0.0, "Basic features"),
    ESSENTIAL("Essential", 9.99, "Essential features for casual users"),
    PREMIUM("Premium", 19.99, "Premium features for active daters"),
    VIP("VIP", 39.99, "VIP experience for power users");

    private final String displayName;
    private final Double monthlyPrice;
    private final String description;

    SubscriptionTier(String displayName, Double monthlyPrice, String description) {
        this.displayName = displayName;
        this.monthlyPrice = monthlyPrice;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public Double getMonthlyPrice() { return monthlyPrice; }
    public String getDescription() { return description; }
}
