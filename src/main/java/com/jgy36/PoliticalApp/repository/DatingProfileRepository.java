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
}
