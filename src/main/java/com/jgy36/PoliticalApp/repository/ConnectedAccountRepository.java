package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.ConnectedAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConnectedAccountRepository extends JpaRepository<ConnectedAccount, Long> {
    List<ConnectedAccount> findByUserId(Long userId);

    Optional<ConnectedAccount> findByUserIdAndProvider(Long userId, String provider);

    Optional<ConnectedAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    @Modifying
    @Query("DELETE FROM ConnectedAccount c WHERE c.user.id = :userId")
    void deleteAllByUserId(Long userId);
}
