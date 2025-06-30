package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.Community;
import com.jgy36.PoliticalApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Long> {

    // Find community by slug (URL identifier)
    Optional<Community> findBySlug(String slug);

    // Check if slug already exists
    boolean existsBySlug(String slug);

    // Find communities by name containing the search term
    List<Community> findByNameContainingIgnoreCase(String searchTerm);

    // Find communities by description containing the search term
    List<Community> findByDescriptionContainingIgnoreCase(String searchTerm);

    // Find communities that user is a member of
    @Query("SELECT c FROM Community c JOIN c.members m WHERE m = :user ORDER BY c.name ASC")
    List<Community> findCommunitiesByMember(@Param("user") User user);

    // Find trending communities (those with most recent posts)
    @Query("SELECT c FROM Community c JOIN c.posts p GROUP BY c ORDER BY MAX(p.createdAt) DESC")
    List<Community> findTrendingCommunities();

    // Find popular communities (those with most members)
    @Query("SELECT c FROM Community c JOIN c.members m GROUP BY c ORDER BY COUNT(m) DESC")
    List<Community> findPopularCommunities();

    // Search communities by name or description
    @Query("SELECT c FROM Community c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Community> searchCommunities(@Param("searchTerm") String searchTerm);

    // Find communities moderated by user
    @Query("SELECT c FROM Community c JOIN c.moderators m WHERE m = :user")
    List<Community> findCommunitiesModeratedBy(@Param("user") User user);

    // Method needed for search functionality
    List<Community> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);

    // Optional: Add methods for finding trending/popular communities
    List<Community> findTop10ByOrderByMembersDesc();
}
