package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.dto.SubscriptionDTO;
import com.jgy36.PoliticalApp.entity.Subscription;
import com.jgy36.PoliticalApp.entity.SubscriptionStatus;
import com.jgy36.PoliticalApp.entity.SubscriptionTier;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.repository.SubscriptionRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentService paymentService;

    /**
     * Get or create subscription for user
     */
    public Subscription getOrCreateSubscription(User user) {
        Optional<Subscription> existing = subscriptionRepository.findByUser(user);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new free subscription
        Subscription subscription = new Subscription(user);
        return subscriptionRepository.save(subscription);
    }

    /**
     * Get current user's subscription
     */
    public Subscription getCurrentUserSubscription() {
        User currentUser = getCurrentUser();
        return getOrCreateSubscription(currentUser);
    }

    /**
     * Convert subscription to DTO with feature limits
     */
    public SubscriptionDTO toDTO(Subscription subscription) {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setId(subscription.getId());
        dto.setTier(subscription.getTier());
        dto.setStatus(subscription.getStatus());
        dto.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
        dto.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        dto.setTrialEnd(subscription.getTrialEnd());
        dto.setCancelAtPeriodEnd(subscription.getCancelAtPeriodEnd());
        dto.setDisplayName(subscription.getTier().getDisplayName());
        dto.setMonthlyPrice(subscription.getTier().getMonthlyPrice());
        dto.setDescription(subscription.getTier().getDescription());

        // Usage data
        dto.setDailySwipesUsed(subscription.getDailySwipesUsed());
        dto.setDailySuperLikesUsed(subscription.getDailySuperLikesUsed());
        dto.setMonthlyBoostsUsed(subscription.getMonthlyBoostsUsed());

        // Set limits based on tier
        setFeatureLimits(dto, subscription.getTier());

        return dto;
    }

    private void setFeatureLimits(SubscriptionDTO dto, SubscriptionTier tier) {
        switch (tier) {
            case FREE:
                dto.setDailySwipeLimit(50);
                dto.setDailySuperLikeLimit(1);
                dto.setMonthlyBoostLimit(0);
                break;
            case ESSENTIAL:
                dto.setDailySwipeLimit(-1); // unlimited
                dto.setDailySuperLikeLimit(3);
                dto.setMonthlyBoostLimit(1);
                break;
            case PREMIUM:
                dto.setDailySwipeLimit(-1); // unlimited
                dto.setDailySuperLikeLimit(5);
                dto.setMonthlyBoostLimit(3);
                break;
            case VIP:
                dto.setDailySwipeLimit(-1); // unlimited
                dto.setDailySuperLikeLimit(10);
                dto.setMonthlyBoostLimit(4);
                break;
        }
    }

    /**
     * Check if user can perform an action based on subscription
     */
    public boolean canPerformAction(String action) {
        Subscription subscription = getCurrentUserSubscription();
        resetUsageIfNeeded(subscription);

        switch (action) {
            case "swipe":
                return canSwipe(subscription);
            case "super_like":
                return canSuperLike(subscription);
            case "boost":
                return canBoost(subscription);
            case "see_likes":
                return subscription.getTier().ordinal() >= SubscriptionTier.PREMIUM.ordinal();
            case "passport_mode":
                return subscription.getTier().ordinal() >= SubscriptionTier.PREMIUM.ordinal();
            case "message_before_match":
                return subscription.getTier().ordinal() >= SubscriptionTier.VIP.ordinal();
            default:
                return true;
        }
    }

    private boolean canSwipe(Subscription subscription) {
        if (subscription.getTier() == SubscriptionTier.FREE) {
            return subscription.getDailySwipesUsed() < 50;
        }
        return true; // Unlimited for paid tiers
    }

    private boolean canSuperLike(Subscription subscription) {
        int limit = getSuperLikeLimit(subscription.getTier());
        return subscription.getDailySuperLikesUsed() < limit;
    }

    private boolean canBoost(Subscription subscription) {
        int limit = getBoostLimit(subscription.getTier());
        return subscription.getMonthlyBoostsUsed() < limit;
    }

    private int getSuperLikeLimit(SubscriptionTier tier) {
        switch (tier) {
            case FREE: return 1;
            case ESSENTIAL: return 3;
            case PREMIUM: return 5;
            case VIP: return 10;
            default: return 0;
        }
    }

    private int getBoostLimit(SubscriptionTier tier) {
        switch (tier) {
            case FREE: return 0;
            case ESSENTIAL: return 1;
            case PREMIUM: return 3;
            case VIP: return 4;
            default: return 0;
        }
    }

    /**
     * Increment usage counter for an action
     */
    public void incrementUsage(String action) {
        Subscription subscription = getCurrentUserSubscription();
        resetUsageIfNeeded(subscription);

        switch (action) {
            case "swipe":
                subscription.setDailySwipesUsed(subscription.getDailySwipesUsed() + 1);
                break;
            case "super_like":
                subscription.setDailySuperLikesUsed(subscription.getDailySuperLikesUsed() + 1);
                break;
            case "boost":
                subscription.setMonthlyBoostsUsed(subscription.getMonthlyBoostsUsed() + 1);
                break;
        }

        subscriptionRepository.save(subscription);
    }

    /**
     * Reset daily/monthly usage if needed
     */
    private void resetUsageIfNeeded(Subscription subscription) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastReset = subscription.getLastResetDate();

        // Reset daily counters
        if (lastReset == null || !lastReset.toLocalDate().equals(now.toLocalDate())) {
            subscription.setDailySwipesUsed(0);
            subscription.setDailySuperLikesUsed(0);
            subscription.setLastResetDate(now);
        }

        // Reset monthly counters
        if (lastReset == null ||
                lastReset.getMonth() != now.getMonth() ||
                lastReset.getYear() != now.getYear()) {
            subscription.setMonthlyBoostsUsed(0);
        }

        subscriptionRepository.save(subscription);
    }

    /**
     * Upgrade subscription
     */
    public Subscription upgradeSubscription(SubscriptionTier newTier, String stripePriceId) {
        User currentUser = getCurrentUser();
        Subscription subscription = getOrCreateSubscription(currentUser);

        try {
            // Create Stripe subscription
            String stripeSubscriptionId = paymentService.createSubscription(
                    subscription.getStripeCustomerId(), stripePriceId);

            // Update subscription
            subscription.setTier(newTier);
            subscription.setStripePriceId(stripePriceId);
            subscription.setStripeSubscriptionId(stripeSubscriptionId);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setCurrentPeriodStart(LocalDateTime.now());
            subscription.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));

            // Set trial period for new users
            if (subscription.getTrialEnd() == null) {
                subscription.setTrialEnd(LocalDateTime.now().plusDays(7));
            }

            return subscriptionRepository.save(subscription);
        } catch (Exception e) {
            logger.error("Failed to upgrade subscription for user {}: {}",
                    currentUser.getId(), e.getMessage());
            throw new RuntimeException("Failed to upgrade subscription: " + e.getMessage());
        }
    }

    /**
     * Cancel subscription
     */
    public Subscription cancelSubscription() {
        User currentUser = getCurrentUser();
        Subscription subscription = getOrCreateSubscription(currentUser);

        if (subscription.getStripeSubscriptionId() != null) {
            try {
                paymentService.cancelSubscription(subscription.getStripeSubscriptionId());
                subscription.setCancelAtPeriodEnd(true);
                return subscriptionRepository.save(subscription);
            } catch (Exception e) {
                logger.error("Failed to cancel subscription for user {}: {}",
                        currentUser.getId(), e.getMessage());
                throw new RuntimeException("Failed to cancel subscription: " + e.getMessage());
            }
        }

        // For free tier, just mark as canceled
        subscription.setStatus(SubscriptionStatus.CANCELED);
        return subscriptionRepository.save(subscription);
    }

    /**
     * Process Stripe webhook
     */
    public void processStripeWebhook(String eventType, String subscriptionId, Object data) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(subscriptionId);

        if (subscriptionOpt.isEmpty()) {
            logger.warn("Received webhook for unknown subscription: {}", subscriptionId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();

        switch (eventType) {
            case "customer.subscription.updated":
                handleSubscriptionUpdated(subscription, data);
                break;
            case "customer.subscription.deleted":
                handleSubscriptionDeleted(subscription);
                break;
            case "invoice.payment_succeeded":
                handlePaymentSucceeded(subscription, data);
                break;
            case "invoice.payment_failed":
                handlePaymentFailed(subscription, data);
                break;
            default:
                logger.info("Unhandled webhook event: {}", eventType);
        }
    }

    private void handleSubscriptionUpdated(Subscription subscription, Object data) {
        // Update subscription based on Stripe data
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);
        logger.info("Updated subscription {} from Stripe webhook", subscription.getId());
    }

    private void handleSubscriptionDeleted(Subscription subscription) {
        subscription.setTier(SubscriptionTier.FREE);
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setStripeSubscriptionId(null);
        subscriptionRepository.save(subscription);
        logger.info("Canceled subscription {} from Stripe webhook", subscription.getId());
    }

    private void handlePaymentSucceeded(Subscription subscription, Object data) {
        // Extend subscription period
        subscription.setCurrentPeriodStart(LocalDateTime.now());
        subscription.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);
        logger.info("Payment succeeded for subscription {}", subscription.getId());
    }

    private void handlePaymentFailed(Subscription subscription, Object data) {
        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        subscriptionRepository.save(subscription);
        logger.warn("Payment failed for subscription {}", subscription.getId());
    }

    /**
     * Scheduled task to check for expired subscriptions
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void checkExpiredSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> expired = subscriptionRepository.findExpiredSubscriptions(now);

        for (Subscription subscription : expired) {
            if (!subscription.getCancelAtPeriodEnd()) {
                // Try to renew
                try {
                    paymentService.renewSubscription(subscription.getStripeSubscriptionId());
                } catch (Exception e) {
                    logger.warn("Failed to renew subscription {}: {}",
                            subscription.getId(), e.getMessage());
                    subscription.setStatus(SubscriptionStatus.PAST_DUE);
                    subscriptionRepository.save(subscription);
                }
            } else {
                // Cancel subscription
                subscription.setTier(SubscriptionTier.FREE);
                subscription.setStatus(SubscriptionStatus.CANCELED);
                subscription.setStripeSubscriptionId(null);
                subscriptionRepository.save(subscription);
            }
        }

        if (!expired.isEmpty()) {
            logger.info("Processed {} expired subscriptions", expired.size());
        }
    }

    /**
     * Get subscription statistics
     */
    public SubscriptionStats getSubscriptionStats() {
        SubscriptionStats stats = new SubscriptionStats();
        stats.setTotalFree(subscriptionRepository.countByTier(SubscriptionTier.FREE));
        stats.setTotalEssential(subscriptionRepository.countByTier(SubscriptionTier.ESSENTIAL));
        stats.setTotalPremium(subscriptionRepository.countByTier(SubscriptionTier.PREMIUM));
        stats.setTotalVip(subscriptionRepository.countByTier(SubscriptionTier.VIP));
        return stats;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    // Inner class for statistics
    public static class SubscriptionStats {
        private long totalFree;
        private long totalEssential;
        private long totalPremium;
        private long totalVip;

        // Getters and setters
        public long getTotalFree() { return totalFree; }
        public void setTotalFree(long totalFree) { this.totalFree = totalFree; }

        public long getTotalEssential() { return totalEssential; }
        public void setTotalEssential(long totalEssential) { this.totalEssential = totalEssential; }

        public long getTotalPremium() { return totalPremium; }
        public void setTotalPremium(long totalPremium) { this.totalPremium = totalPremium; }

        public long getTotalVip() { return totalVip; }
        public void setTotalVip(long totalVip) { this.totalVip = totalVip; }
    }
}
