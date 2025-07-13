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

    // ==================== YOUR ORIGINAL METHODS (KEEP THESE) ====================

    Optional<DatingProfile> findByUser(User user);

    @Query("SELECT dp FROM DatingProfile dp WHERE dp.isActive = true AND dp.user.id != :userId")
    List<DatingProfile> findActiveDatingProfilesExcludingUser(@Param("userId") Long userId);

    // ✅ YOUR ORIGINAL - Better age/dating logic
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

    // ✅ YOUR ORIGINAL - Better card stack logic
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

    // ==================== NEW METHODS FOR ADVANCED FILTERS (FIXED) ====================

    /**
     * Potential matches with advanced filters - ONLY using existing fields
     */
    @Query("SELECT dp FROM DatingProfile dp " +
            "WHERE dp.user.id != :userId " +
            "AND dp.isActive = true " +
            "AND dp.user.ageConfirmed = true " +
            "AND dp.user.dateOfBirth IS NOT NULL " +
            "AND (" +
            "    :genderPreference IS NULL OR " +
            "    :genderPreference = 'EVERYONE' OR " +
            "    (:genderPreference = 'MEN' AND dp.gender = 'MAN') OR " +
            "    (:genderPreference = 'WOMEN' AND dp.gender = 'WOMAN') OR " +
            "    (:genderPreference = 'NON_BINARY' AND dp.gender = 'NON_BINARY')" +
            ") " +
            "AND EXTRACT(YEAR FROM CURRENT_DATE) - EXTRACT(YEAR FROM dp.user.dateOfBirth) >= :minAge " +
            "AND EXTRACT(YEAR FROM CURRENT_DATE) - EXTRACT(YEAR FROM dp.user.dateOfBirth) <= :maxAge " +
            "AND (:location IS NULL OR dp.location = :location) " +
            "AND (:lifestyle IS NULL OR dp.lifestyle = :lifestyle) " +
            "AND (:religion IS NULL OR dp.religion = :religion) " +
            "AND (:relationshipType IS NULL OR dp.relationshipType = :relationshipType) " +
            "AND (:drinking IS NULL OR dp.drinking = :drinking) " +
            "AND (:smoking IS NULL OR dp.smoking = :smoking) " +
            "AND (:hasChildren IS NULL OR dp.hasChildren = :hasChildren) " +
            "AND (:wantChildren IS NULL OR dp.wantChildren = :wantChildren) " +
            "AND dp.user.id NOT IN (" +
            "   SELECT s.target.id FROM Swipe s WHERE s.swiper.id = :userId" +
            ")")
    List<DatingProfile> findPotentialMatchesWithFilters(
            @Param("userId") Long userId,
            @Param("genderPreference") String genderPreference,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            @Param("location") String location,
            @Param("lifestyle") String lifestyle,
            @Param("religion") String religion,
            @Param("relationshipType") String relationshipType,
            @Param("drinking") String drinking,
            @Param("smoking") String smoking,
            @Param("hasChildren") String hasChildren,
            @Param("wantChildren") String wantChildren
    );

    /**
     * Eligible profiles for card stack algorithm with filters - ONLY using existing fields
     */
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
        AND (:location IS NULL OR dp.location = :location)
        AND (:lifestyle IS NULL OR dp.lifestyle = :lifestyle)
        AND (:religion IS NULL OR dp.religion = :religion)
        AND (:relationshipType IS NULL OR dp.relationshipType = :relationshipType)
        AND (:drinking IS NULL OR dp.drinking = :drinking)
        AND (:smoking IS NULL OR dp.smoking = :smoking)
        AND (:hasChildren IS NULL OR dp.hasChildren = :hasChildren)
        AND (:wantChildren IS NULL OR dp.wantChildren = :wantChildren)
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
    List<DatingProfile> findEligibleProfilesForCardStackWithFilters(
            @Param("userId") Long userId,
            @Param("genderPreference") String genderPreference,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            @Param("location") String location,
            @Param("lifestyle") String lifestyle,
            @Param("religion") String religion,
            @Param("relationshipType") String relationshipType,
            @Param("drinking") String drinking,
            @Param("smoking") String smoking,
            @Param("hasChildren") String hasChildren,
            @Param("wantChildren") String wantChildren
    );
}
