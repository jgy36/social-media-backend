package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.Swipe;
import com.jgy36.PoliticalApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SwipeRepository extends JpaRepository<Swipe, Long> {
    Optional<Swipe> findBySwiperAndTarget(User swiper, User target);
    List<Swipe> findBySwiper(User swiper);
    boolean existsBySwiperAndTarget(User swiper, User target);
}
