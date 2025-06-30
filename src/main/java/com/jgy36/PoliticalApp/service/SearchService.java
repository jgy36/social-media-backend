package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.dto.SearchResultDTO;
import com.jgy36.PoliticalApp.entity.Community;
import com.jgy36.PoliticalApp.entity.Post;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.repository.CommunityRepository;
import com.jgy36.PoliticalApp.repository.PostRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final Logger logger = Logger.getLogger(SearchService.class.getName());

    private final UserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final PostRepository postRepository;

    @Autowired
    public SearchService(
            UserRepository userRepository,
            CommunityRepository communityRepository,
            PostRepository postRepository) {
        this.userRepository = userRepository;
        this.communityRepository = communityRepository;
        this.postRepository = postRepository;
    }

    /**
     * Search for results across all supported types
     */
    public List<SearchResultDTO> searchAll(String query) {
        logger.info("Performing search across all types for query: " + query);
        List<SearchResultDTO> results = new ArrayList<>();

        try {
            // Search users
            results.addAll(searchUsers(query));

            // Search communities
            results.addAll(searchCommunities(query));

            // Search posts (hashtags can be derived from posts)
            results.addAll(searchPosts(query));

            // Search hashtags directly if you have a dedicated repository
            // results.addAll(searchHashtags(query));

        } catch (Exception e) {
            logger.severe("Error in searchAll: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Search by specific type
     */
    public List<SearchResultDTO> searchByType(String query, String type) {
        logger.info("Performing search for type: " + type + " with query: " + query);

        return switch (type.toLowerCase()) {
            case "user" -> searchUsers(query);
            case "community" -> searchCommunities(query);
            case "post" -> searchPosts(query);
            case "hashtag" -> searchHashtags(query);
            default -> new ArrayList<>();
        };
    }

    /**
     * Search for users matching the query
     */
    private List<SearchResultDTO> searchUsers(String query) {
        try {
            List<User> users = userRepository.findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
                    query, query);

            return users.stream()
                    .map(user -> {
                        SearchResultDTO result = new SearchResultDTO();
                        result.setId(user.getId().toString());
                        result.setType("user");
                        result.setName(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
                        result.setUsername(user.getUsername());
                        result.setBio(user.getBio());

                        // Use the repository method to get followers count
                        int followersCount = userRepository.countFollowers(user.getId());
                        result.setFollowersCount(followersCount);

                        return result;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.severe("Error searching users: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Search for communities matching the query
     */
    private List<SearchResultDTO> searchCommunities(String query) {
        try {
            List<Community> communities = communityRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    query, query);

            return communities.stream()
                    .map(community -> {
                        SearchResultDTO result = new SearchResultDTO();
                        result.setId(community.getSlug());
                        result.setType("community");
                        result.setName(community.getName());
                        result.setDescription(community.getDescription());
                        result.setMembers(community.getMembers().size());
                        return result;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.severe("Error searching communities: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Search for posts matching the query
     */
    private List<SearchResultDTO> searchPosts(String query) {
        try {
            List<Post> posts = postRepository.findByContentContainingIgnoreCase(query);

            return posts.stream()
                    .map(post -> {
                        SearchResultDTO result = new SearchResultDTO();
                        result.setId(post.getId().toString());
                        result.setType("post");
                        result.setContent(post.getContent());
                        result.setAuthor(post.getAuthor().getUsername());
                        result.setCreatedAt(post.getCreatedAt().toString());
                        return result;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.severe("Error searching posts: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Search for hashtags matching the query
     * Note: This is a simplified implementation that extracts hashtags from posts
     */
    private List<SearchResultDTO> searchHashtags(String query) {
        try {
            // Remove # if present in the query
            String cleanQuery = query.startsWith("#") ? query.substring(1) : query;

            // Find posts with the hashtag
            List<Post> posts = postRepository.findByContentContainingIgnoreCase("#" + cleanQuery);

            // Only return a result if we found any posts
            if (!posts.isEmpty()) {
                SearchResultDTO result = new SearchResultDTO();
                result.setId(cleanQuery);
                result.setType("hashtag");
                result.setName("#" + cleanQuery);
                result.setTag("#" + cleanQuery);
                result.setPostCount(posts.size());
                result.setCount(posts.size());
                return List.of(result);
            }

            return new ArrayList<>();
        } catch (Exception e) {
            logger.severe("Error searching hashtags: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
