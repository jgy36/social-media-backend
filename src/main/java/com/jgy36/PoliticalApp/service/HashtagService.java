package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.entity.Hashtag;
import com.jgy36.PoliticalApp.entity.Post;
import com.jgy36.PoliticalApp.repository.HashtagRepository;
import com.jgy36.PoliticalApp.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HashtagService {

    private final HashtagRepository hashtagRepository;
    private final PostRepository postRepository;

    public HashtagService(HashtagRepository hashtagRepository, PostRepository postRepository) {
        this.hashtagRepository = hashtagRepository;
        this.postRepository = postRepository;
    }

    /**
     * Gets all hashtags used in the system
     *
     * @return List of all hashtags
     */
    public List<Hashtag> getAllHashtags() {
        return hashtagRepository.findAll();
    }

    /**
     * Search for hashtags containing the query
     *
     * @param query The search query
     * @return List of matching hashtags
     */
    public List<Hashtag> searchHashtags(String query) {
        // Log the search for debugging
        System.out.println("🔍 Searching hashtags with query: " + query);

        // Clean up the query - search with or without # prefix
        String searchTerm = query;
        if (query.startsWith("#")) {
            searchTerm = query.substring(1);
        }

        // First try the specific method if available
        try {
            List<Hashtag> results = hashtagRepository.findByTagContainingIgnoreCase(searchTerm);
            System.out.println("✅ Found " + results.size() + " hashtags matching: " + query);
            return results;
        } catch (Exception e) {
            System.out.println("⚠️ Error using findByTagContainingIgnoreCase: " + e.getMessage());

            // Fall back to the query method
            List<Hashtag> results = hashtagRepository.searchHashtags(searchTerm);
            System.out.println("✅ Found " + results.size() + " hashtags with query method for: " + query);
            return results;
        }
    }

    /**
     * Gets trending hashtags (most used)
     *
     * @return List of trending hashtags
     */
    public List<Hashtag> getTrendingHashtags() {
        // Default to top 10
        return getTrendingHashtags(10);
    }

    /**
     * Gets trending hashtags with a limit
     *
     * @param limit Number of hashtags to return
     * @return List of trending hashtags with counts
     */
    public List<Hashtag> getTrendingHashtags(int limit) {
        // Get all hashtags
        List<Hashtag> allTags = hashtagRepository.findAll();

        // Sort by count in descending order and limit the results
        return allTags.stream()
                .sorted(Comparator.comparing(Hashtag::getCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Gets information about a hashtag
     *
     * @param hashtag The hashtag to get info for
     * @return A count of how many times the hashtag is used
     */
    @Transactional(readOnly = true)
    public int getHashtagCount(String hashtag) {
        // Add # symbol if not present
        final String tagText = hashtag.startsWith("#") ? hashtag : "#" + hashtag;

        // Try to find the hashtag in the database
        Optional<Hashtag> hashtagOpt = hashtagRepository.findByTag(tagText);

        if (hashtagOpt.isPresent()) {
            return hashtagOpt.get().getCount();
        }

        // If not found in database, count posts containing this hashtag
        List<Post> posts = postRepository.findByContentContainingIgnoreCase(tagText);
        return posts.size();
    }
}
