package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.Follow;
import com.jgy36.PoliticalApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    boolean existsByFollowerAndFollowing(User follower, User following);

    @Transactional
    void deleteByFollowerAndFollowing(User follower, User following);

    List<Follow> findByFollower(User follower);

    // Find all users who follow a given user (their followers)
    @Query("SELECT f.follower FROM Follow f WHERE f.following.id = :userId")
    List<User> findFollowersByFollowingId(@Param("userId") Long userId);

    // Find all users a given user follows (their following)
    @Query("SELECT f.following FROM Follow f WHERE f.follower.id = :userId")
    List<User> findFollowingByFollowerId(@Param("userId") Long userId);

    // Count how many followers a user has
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.following.id = :userId")
    int countFollowersByFollowingId(@Param("userId") Long userId);

    // Count how many users a user follows
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.follower.id = :userId")
    int countFollowingByFollowerId(@Param("userId") Long userId);
}
