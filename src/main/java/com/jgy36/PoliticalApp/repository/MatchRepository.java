package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.Match;
import com.jgy36.PoliticalApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    @Query("SELECT m FROM Match m WHERE (m.user1 = :user OR m.user2 = :user) AND m.isActive = true")
    List<Match> findActiveMatchesForUser(@Param("user") User user);

    @Query("SELECT m FROM Match m WHERE ((m.user1 = :user1 AND m.user2 = :user2) OR (m.user1 = :user2 AND m.user2 = :user1)) AND m.isActive = true")
    Optional<Match> findActiveMatchBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);
}
