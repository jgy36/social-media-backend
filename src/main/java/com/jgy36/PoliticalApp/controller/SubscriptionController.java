package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.dto.SubscriptionDTO;
import com.jgy36.PoliticalApp.entity.SubscriptionTier;
import com.jgy36.PoliticalApp.service.PaymentService;
import com.jgy36.PoliticalApp.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@CrossOrigin(origins = "http://localhost:3000")
public class SubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private PaymentService paymentService;

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    @Value("${app.subscription.essential.price-id}")
    private String essentialPriceId;

    @Value("${app.subscription.premium.price-id}")
    private String premiumPriceId;

    @Value("${app.subscription.vip.price-id}")
    private String vipPriceId;

    /**
     * Get current user's subscription details
     */
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubscriptionDTO> getCurrentSubscription() {
        try {
            var subscription = subscriptionService.getCurrentUserSubscription();
            SubscriptionDTO dto = subscriptionService.toDTO(subscription);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("Error getting current subscription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get subscription tiers and pricing
     */
    @GetMapping("/tiers")
    public ResponseEntity<Map<String, Object>> getSubscriptionTiers() {
        Map<String, Object> response = new HashMap<>();

        Map<String, Object> tiers = new HashMap<>();

        // Free tier
        Map<String, Object> free = new HashMap<>();
        free.put("name", "Free");
        free.put("price", 0.0);
        free.put("description", "Basic features");
        free.put("features", Map.of(
                "dailySwipes", 50,
                "superLikesPerDay", 1,
                "monthlyBoosts", 0,
                "seeWhoLikedYou", false,
                "passportMode", false,
                "adFree", false,
                "undoSwipes", false
        ));

        // Essential tier
        Map<String, Object> essential = new HashMap<>();
        essential.put("name", "Essential");
        essential.put("price", 9.99);
        essential.put("description", "Perfect for casual users");
        essential.put("priceId", essentialPriceId);
        essential.put("features", Map.of(
                "dailySwipes", "unlimited",
                "superLikesPerDay", 3,
                "monthlyBoosts", 1,
                "seeWhoLikedYou", "partial", // 5 recent likes
                "passportMode", false,
                "adFree", true,
                "undoSwipes", true
        ));

        // Premium tier
        Map<String, Object> premium = new HashMap<>();
        premium.put("name", "Premium");
        premium.put("price", 19.99);
        premium.put("description", "For active daters");
        premium.put("priceId", premiumPriceId);
        premium.put("features", Map.of(
                "dailySwipes", "unlimited",
                "superLikesPerDay", 5,
                "monthlyBoosts", 3,
                "seeWhoLikedYou", true,
                "passportMode", true,
                "adFree", true,
                "undoSwipes", true,
                "advancedFilters", true,
                "readReceipts", true,
                "priorityDisplay", true
        ));

        // VIP tier
        Map<String, Object> vip = new HashMap<>();
        vip.put("name", "VIP");
        vip.put("price", 39.99);
        vip.put("description", "Elite experience");
        vip.put("priceId", vipPriceId);
        Map<String, Object> vipFeatures = new HashMap<>();
        vipFeatures.put("dailySwipes", "unlimited");
        vipFeatures.put("superLikesPerDay", 10);
        vipFeatures.put("monthlyBoosts", 4);
        vipFeatures.put("seeWhoLikedYou", true);
        vipFeatures.put("passportMode", true);
        vipFeatures.put("adFree", true);
        vipFeatures.put("undoSwipes", true);
        vipFeatures.put("advancedFilters", true);
        vipFeatures.put("readReceipts", true);
        vipFeatures.put("priorityDisplay", true);
        vipFeatures.put("messageBeforeMatch", true);
        vipFeatures.put("vipBadge", true);
        vipFeatures.put("prioritySupport", true);
        vipFeatures.put("profileReviews", true);

        vip.put("features", vipFeatures);

        tiers.put("free", free);
        tiers.put("essential", essential);
        tiers.put("premium", premium);
        tiers.put("vip", vip);

        response.put("tiers", tiers);
        response.put("stripePublishableKey", stripePublishableKey);

        return ResponseEntity.ok(response);
    }

    /**
     * Create payment setup intent for subscription
     */
    @PostMapping("/setup-payment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> setupPayment() {
        try {
            var subscription = subscriptionService.getCurrentUserSubscription();

            // Create Stripe customer if doesn't exist
            if (subscription.getStripeCustomerId() == null) {
                var user = subscription.getUser();
                String customerId = paymentService.createCustomer(
                        user.getEmail(),
                        user.getDisplayName() != null ? user.getDisplayName() : user.getUsername()
                );
                subscription.setStripeCustomerId(customerId);
                // Save subscription with customer ID
            }

            // Create setup intent
            String clientSecret = paymentService.createSetupIntent(subscription.getStripeCustomerId());

            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", clientSecret);
            response.put("customerId", subscription.getStripeCustomerId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error setting up payment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Upgrade to a new subscription tier
     */
    @PostMapping("/upgrade")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> upgradeSubscription(
            @RequestBody Map<String, String> request) {
        try {
            String tierString = request.get("tier");
            String paymentMethodId = request.get("paymentMethodId");

            SubscriptionTier newTier;
            String priceId;

            switch (tierString.toUpperCase()) {
                case "ESSENTIAL":
                    newTier = SubscriptionTier.ESSENTIAL;
                    priceId = essentialPriceId;
                    break;
                case "PREMIUM":
                    newTier = SubscriptionTier.PREMIUM;
                    priceId = premiumPriceId;
                    break;
                case "VIP":
                    newTier = SubscriptionTier.VIP;
                    priceId = vipPriceId;
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid subscription tier"));
            }

            var updatedSubscription = subscriptionService.upgradeSubscription(newTier, priceId);
            SubscriptionDTO dto = subscriptionService.toDTO(updatedSubscription);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully upgraded to " + newTier.getDisplayName());
            response.put("subscription", dto);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error upgrading subscription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upgrade subscription: " + e.getMessage()));
        }
    }

    /**
     * Cancel subscription
     */
    @PostMapping("/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> cancelSubscription() {
        try {
            var canceledSubscription = subscriptionService.cancelSubscription();
            SubscriptionDTO dto = subscriptionService.toDTO(canceledSubscription);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Subscription will be canceled at the end of the current period");
            response.put("subscription", dto);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error canceling subscription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to cancel subscription: " + e.getMessage()));
        }
    }

    /**
     * Get subscription usage/stats
     */
    @GetMapping("/usage")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getUsageStats() {
        try {
            var subscription = subscriptionService.getCurrentUserSubscription();
            SubscriptionDTO dto = subscriptionService.toDTO(subscription);

            Map<String, Object> usage = new HashMap<>();
            usage.put("dailySwipesUsed", dto.getDailySwipesUsed());
            usage.put("dailySwipeLimit", dto.getDailySwipeLimit());
            usage.put("dailySuperLikesUsed", dto.getDailySuperLikesUsed());
            usage.put("dailySuperLikeLimit", dto.getDailySuperLikeLimit());
            usage.put("monthlyBoostsUsed", dto.getMonthlyBoostsUsed());
            usage.put("monthlyBoostLimit", dto.getMonthlyBoostLimit());

            // Calculate percentages
            if (dto.getDailySwipeLimit() > 0) {
                usage.put("swipeUsagePercent",
                        (dto.getDailySwipesUsed() * 100.0) / dto.getDailySwipeLimit());
            } else {
                usage.put("swipeUsagePercent", 0);
            }

            usage.put("superLikeUsagePercent",
                    (dto.getDailySuperLikesUsed() * 100.0) / dto.getDailySuperLikeLimit());

            if (dto.getMonthlyBoostLimit() > 0) {
                usage.put("boostUsagePercent",
                        (dto.getMonthlyBoostsUsed() * 100.0) / dto.getMonthlyBoostLimit());
            } else {
                usage.put("boostUsagePercent", 0);
            }

            return ResponseEntity.ok(usage);
        } catch (Exception e) {
            logger.error("Error getting usage stats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if user can perform a specific action
     */
    @GetMapping("/can-perform/{action}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> canPerformAction(@PathVariable String action) {
        try {
            boolean canPerform = subscriptionService.canPerformAction(action);
            var subscription = subscriptionService.getCurrentUserSubscription();

            Map<String, Object> response = new HashMap<>();
            response.put("canPerform", canPerform);
            response.put("currentTier", subscription.getTier().getDisplayName());

            if (!canPerform) {
                response.put("reason", "Subscription limit reached or tier insufficient");
                response.put("upgradeRequired", true);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking action permission: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Admin endpoint to get subscription statistics
     */
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionService.SubscriptionStats> getSubscriptionStats() {
        try {
            var stats = subscriptionService.getSubscriptionStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting subscription stats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
