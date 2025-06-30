// Ensure your HashtagRepository.java has these methods:

package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

    // Find hashtag by its exact tag (including # symbol)
    Optional<Hashtag> findByTag(String tag);

    // Check if hashtag exists
    boolean existsByTag(String tag);

    // Find hashtags by partial name match (case insensitive)
    // This is what your controller is using for search
    List<Hashtag> findByTagContainingIgnoreCase(String partialTag);

    // Make sure there's a proper implementation method for your search
    @Query("SELECT h FROM Hashtag h WHERE LOWER(h.tag) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Hashtag> searchHashtags(@Param("searchTerm") String searchTerm);
}
