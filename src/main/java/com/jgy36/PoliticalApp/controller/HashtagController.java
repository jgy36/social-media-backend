package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.dto.PostDTO;
import com.jgy36.PoliticalApp.entity.Hashtag;
import com.jgy36.PoliticalApp.entity.Post;
import com.jgy36.PoliticalApp.service.HashtagService;
import com.jgy36.PoliticalApp.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hashtags")
@CrossOrigin(origins = "*")
public class HashtagController {

    @Autowired
    private HashtagService hashtagService;

    @Autowired
    private PostService postService;

    /**
     * Get all hashtags used in the system
     */
    @GetMapping
    public ResponseEntity<List<Hashtag>> getAllHashtags() {
        List<Hashtag> hashtags = hashtagService.getAllHashtags();
        return ResponseEntity.ok(hashtags);
    }

    /**
     * Get posts containing a specific hashtag
     */
    @GetMapping("/{hashtag}")
    public ResponseEntity<List<PostDTO>> getPostsByHashtag(@PathVariable String hashtag) {
        try {
            // Log the request
            System.out.println("Fetching posts for hashtag: " + hashtag);

            // Get posts by tag
            List<Post> posts = postService.getPostsByTag(hashtag);

            // Convert to DTOs to avoid circular references
            List<PostDTO> postDTOs = posts.stream()
                    .map(PostDTO::new)
                    .collect(Collectors.toList());

            System.out.println("Found " + postDTOs.size() + " posts for hashtag: " + hashtag);

            return ResponseEntity.ok(postDTOs);
        } catch (Exception e) {
            System.err.println("Error fetching posts for hashtag: " + hashtag);
            e.printStackTrace();
            // Return empty list instead of error
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Get trending hashtags (defaults to top 10)
     */
    @GetMapping("/trending")
    public ResponseEntity<List<Hashtag>> getTrendingHashtags() {
        List<Hashtag> trending = hashtagService.getTrendingHashtags();
        return ResponseEntity.ok(trending);
    }

    /**
     * Get trending hashtags with a limit
     */
    @GetMapping("/trending/{limit}")
    public ResponseEntity<List<Hashtag>> getTrendingHashtags(@PathVariable int limit) {
        List<Hashtag> trending = hashtagService.getTrendingHashtags(limit);
        return ResponseEntity.ok(trending);
    }

    /**
     * Search for hashtags containing the search term
     */
    @GetMapping("/search")
    public ResponseEntity<List<Hashtag>> searchHashtags(@RequestParam String query) {
        List<Hashtag> results = hashtagService.searchHashtags(query);
        return ResponseEntity.ok(results);
    }

    /**
     * Get information about a specific hashtag
     */
    @GetMapping("/info/{hashtag}")
    public ResponseEntity<Map<String, Object>> getHashtagInfo(@PathVariable String hashtag) {
        int count = hashtagService.getHashtagCount(hashtag);
        return ResponseEntity.ok(Map.of(
                "tag", hashtag,
                "useCount", count
        ));
    }
}
