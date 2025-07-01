// Add this to: src/main/java/com/jgy36/PoliticalApp/repository/SwipeRepository.java
package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.Swipe;
import com.jgy36.PoliticalApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SwipeRepository extends JpaRepository<Swipe, Long> {

    boolean existsBySwiperAndTarget(User swiper, User target);

    Optional<Swipe> findBySwiperAndTarget(User swiper, User target);

    long countBySwiperAndSwipedAtAfter(User swiper, java.time.LocalDateTime after);
}
