package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.DatingProfile;
import com.jgy36.PoliticalApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatingProfileRepository extends JpaRepository<DatingProfile, Long> {
    Optional<DatingProfile> findByUser(User user);

    @Query("SELECT dp FROM DatingProfile dp WHERE dp.isActive = true AND dp.user.id != :userId")
    List<DatingProfile> findActiveDatingProfilesExcludingUser(@Param("userId") Long userId);

    // ✅ FIXED: Handle null gender preference and use string comparison
    @Query("SELECT dp FROM DatingProfile dp " +
            "WHERE dp.user.id != :userId " +
            "AND dp.isActive = true " +
            "AND dp.user.ageConfirmed = true " +
            "AND dp.user.dateOfBirth IS NOT NULL " +
            "AND (" +
            "    :genderPreferenceStr IS NULL OR " +
            "    :genderPreferenceStr = 'EVERYONE' OR " +
            "    (:genderPreferenceStr = 'MEN' AND dp.gender = 'MAN') OR " +
            "    (:genderPreferenceStr = 'WOMEN' AND dp.gender = 'WOMAN') OR " +
            "    (:genderPreferenceStr = 'NON_BINARY' AND dp.gender = 'NON_BINARY')" +
            ") " +
            "AND EXTRACT(YEAR FROM CURRENT_DATE) - EXTRACT(YEAR FROM dp.user.dateOfBirth) >= :minAge " +
            "AND EXTRACT(YEAR FROM CURRENT_DATE) - EXTRACT(YEAR FROM dp.user.dateOfBirth) <= :maxAge")
    List<DatingProfile> findPotentialMatches(
            @Param("userId") Long userId,
            @Param("genderPreferenceStr") String genderPreferenceStr,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge
    );

    // ✅ FIXED: Card stack algorithm query with proper string to enum comparison
    @Query("""
        SELECT DISTINCT dp FROM DatingProfile dp 
        WHERE dp.user.id != :userId 
        AND dp.isActive = true 
        AND dp.user.datingModeEnabled = true 
        AND dp.user.ageConfirmed = true
        AND (dp.age BETWEEN :minAge AND :maxAge)
        AND (
            :genderPreference = 'EVERYONE' OR 
            (:genderPreference = 'MEN' AND dp.gender = 'MAN') OR
            (:genderPreference = 'WOMEN' AND dp.gender = 'WOMAN') OR
            (:genderPreference = 'NON_BINARY' AND dp.gender = 'NON_BINARY')
        )
        AND NOT EXISTS (
            SELECT s FROM Swipe s 
            WHERE s.swiper.id = :userId 
            AND s.target.id = dp.user.id 
            AND NOT EXISTS (
                SELECT s2 FROM Swipe s2 
                WHERE s2.swiper.id = dp.user.id 
                AND s2.target.id = :userId 
                AND s2.direction IN ('LIKE', 'SUPER_LIKE')
                AND s2.swipedAt > s.swipedAt
            )
        )
        ORDER BY dp.user.lastActive DESC
        """)
    List<DatingProfile> findEligibleProfilesForCardStack(
            @Param("userId") Long userId,
            @Param("genderPreference") String genderPreference,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge
    );
}
