package com.jgy36.PoliticalApp.aspect;

import com.jgy36.PoliticalApp.annotation.RequireSubscription;
import com.jgy36.PoliticalApp.entity.SubscriptionTier;
import com.jgy36.PoliticalApp.service.SubscriptionService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Aspect
@Component
public class SubscriptionAspect {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionAspect.class);

    @Autowired
    private SubscriptionService subscriptionService;

    @Around("@annotation(requireSubscription)")
    public Object checkSubscription(ProceedingJoinPoint joinPoint, RequireSubscription requireSubscription) throws Throwable {
        try {
            // Check if user has required subscription tier
            var subscription = subscriptionService.getCurrentUserSubscription();
            SubscriptionTier requiredTier = requireSubscription.tier();
            SubscriptionTier userTier = subscription.getTier();

            // Check if user meets the requirement
            if (userTier.ordinal() < requiredTier.ordinal()) {
                logger.warn("User with tier {} attempted to access feature requiring {}",
                        userTier, requiredTier);

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "error", requireSubscription.message(),
                                "requiredTier", requiredTier.getDisplayName(),
                                "currentTier", userTier.getDisplayName(),
                                "upgradeRequired", true
                        ));
            }

            // Check specific feature if specified
            String feature = requireSubscription.feature();
            if (!feature.isEmpty() && !subscriptionService.canPerformAction(feature)) {
                logger.warn("User exceeded usage limits for feature: {}", feature);

                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of(
                                "error", "You've reached your limit for this feature",
                                "feature", feature,
                                "upgradeRequired", true
                        ));
            }

            // Proceed with the method execution
            Object result = joinPoint.proceed();

            // Increment usage counter if feature is specified
            if (!feature.isEmpty()) {
                subscriptionService.incrementUsage(feature);
            }

            return result;

        } catch (Exception e) {
            logger.error("Error in subscription aspect: {}", e.getMessage());
            throw e;
        }
    }
}
