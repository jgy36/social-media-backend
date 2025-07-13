package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.Swipe;
import com.jgy36.PoliticalApp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SwipeRepository extends JpaRepository<Swipe, Long> {

    /**
     * Check if a swiper has already swiped on a target user
     */
    boolean existsBySwiperAndTarget(User swiper, User target);

    /**
     * Find a swipe between two specific users
     */
    Optional<Swipe> findBySwiperAndTarget(User swiper, User target);

    /**
     * Count swipes by a user after a specific date (for daily limits)
     */
    long countBySwiperAndSwipedAtAfter(User swiper, LocalDateTime after);

    /**
     * Count super likes by a user after a specific date (for daily limits)
     */
    @Query("SELECT COUNT(s) FROM Swipe s WHERE s.swiper = :swiper AND s.swipedAt > :after AND s.direction = 'SUPER_LIKE'")
    long countSuperLikesBySwiperAndSwipedAtAfter(@Param("swiper") User swiper, @Param("after") LocalDateTime after);

    /**
     * Find all likes received by a user (for "Who Liked Me" feature)
     */
    @Query("SELECT s FROM Swipe s WHERE s.target = :user AND (s.direction = 'LIKE' OR s.direction = 'SUPER_LIKE') ORDER BY s.swipedAt DESC")
    List<Swipe> findLikesReceivedByUser(@Param("user") User user);

    /**
     * Find likes received by a user with pagination
     */
    @Query("SELECT s FROM Swipe s WHERE s.target = :user AND (s.direction = 'LIKE' OR s.direction = 'SUPER_LIKE') ORDER BY s.swipedAt DESC")
    Page<Swipe> findLikesReceivedByUser(@Param("user") User user, Pageable pageable);

    /**
     * Find the most recent swipe by a user (for undo functionality)
     */
    Optional<Swipe> findTopBySwiperOrderBySwipedAtDesc(User swiper);

    /**
     * Find all swipes by a user on a specific day (for analytics)
     */
    @Query("SELECT s FROM Swipe s WHERE s.swiper = :swiper AND DATE(s.swipedAt) = DATE(:date)")
    List<Swipe> findSwipesByUserOnDate(@Param("swiper") User swiper, @Param("date") LocalDateTime date);

    /**
     * Find recent swipes between two users (for match validation)
     */
    @Query("SELECT s FROM Swipe s WHERE s.swiper = :swiper AND s.target = :target AND s.swipedAt > :since ORDER BY s.swipedAt DESC")
    List<Swipe> findRecentSwipesBetweenUsers(
            @Param("swiper") User swiper,
            @Param("target") User target,
            @Param("since") LocalDateTime since
    );

    /**
     * Get super likes received by a user
     */
    @Query("SELECT s FROM Swipe s WHERE s.target = :user AND s.direction = 'SUPER_LIKE' ORDER BY s.swipedAt DESC")
    List<Swipe> findSuperLikesReceivedByUser(@Param("user") User user);

    /**
     * Find mutual likes between users (for match creation)
     */
    @Query("SELECT s1 FROM Swipe s1 WHERE EXISTS (" +
            "SELECT s2 FROM Swipe s2 WHERE " +
            "s1.swiper = s2.target AND s1.target = s2.swiper AND " +
            "(s1.direction = 'LIKE' OR s1.direction = 'SUPER_LIKE') AND " +
            "(s2.direction = 'LIKE' OR s2.direction = 'SUPER_LIKE')" +
            ") AND s1.swiper = :user")
    List<Swipe> findMutualLikesForUser(@Param("user") User user);

    /**
     * Count total swipes by direction for analytics
     */
    @Query("SELECT COUNT(s) FROM Swipe s WHERE s.swiper = :user AND s.direction = :direction")
    long countSwipesByUserAndDirection(@Param("user") User user, @Param("direction") String direction);

    /**
     * Find users who swiped left on a specific user (for algorithm tuning)
     */
    @Query("SELECT s FROM Swipe s WHERE s.target = :user AND s.direction = 'DISLIKE' ORDER BY s.swipedAt DESC")
    List<Swipe> findDislikesReceivedByUser(@Param("user") User user);

    /**
     * Get swipe statistics for a user within a date range
     */
    @Query("SELECT s.direction, COUNT(s) FROM Swipe s WHERE s.swiper = :user AND s.swipedAt BETWEEN :start AND :end GROUP BY s.direction")
    List<Object[]> getSwipeStatsByUserAndDateRange(
            @Param("user") User user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Delete old swipes for cleanup (older than specified date)
     */
    @Query("DELETE FROM Swipe s WHERE s.swipedAt < :cutoffDate")
    void deleteSwipesOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
