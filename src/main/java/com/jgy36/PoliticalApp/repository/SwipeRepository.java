// Add this to: src/main/java/com/jgy36/PoliticalApp/repository/SwipeRepository.java
package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.Swipe;
import com.jgy36.PoliticalApp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;  // ADD THIS IMPORT



import java.util.List;
import java.util.Optional;

@Repository
public interface SwipeRepository extends JpaRepository<Swipe, Long> {

    boolean existsBySwiperAndTarget(User swiper, User target);

    Optional<Swipe> findBySwiperAndTarget(User swiper, User target);

    long countBySwiperAndSwipedAtAfter(User swiper, java.time.LocalDateTime after);

    @Query("SELECT s FROM Swipe s WHERE s.target = :user AND s.direction = 'LIKE' ORDER BY s.swipedAt DESC")
    List<Swipe> findLikesReceivedByUser(@Param("user") User user);

    @Query("SELECT s FROM Swipe s WHERE s.target = :user AND s.direction = 'LIKE' ORDER BY s.swipedAt DESC")
    Page<Swipe> findLikesReceivedByUser(@Param("user") User user, Pageable pageable);
}
