package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.Subscription;
import com.jgy36.PoliticalApp.entity.SubscriptionTier;
import com.jgy36.PoliticalApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUser(User user);

    Optional<Subscription> findByUserId(Long userId);

    Optional<Subscription> findByStripeCustomerId(String stripeCustomerId);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    List<Subscription> findByTier(SubscriptionTier tier);

    @Query("SELECT s FROM Subscription s WHERE s.currentPeriodEnd < :now AND s.status = 'ACTIVE'")
    List<Subscription> findExpiredSubscriptions(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.tier = :tier")
    long countByTier(@Param("tier") SubscriptionTier tier);

    @Query("SELECT s FROM Subscription s WHERE s.trialEnd < :now AND s.tier != 'FREE'")
    List<Subscription> findExpiredTrials(@Param("now") LocalDateTime now);
}
