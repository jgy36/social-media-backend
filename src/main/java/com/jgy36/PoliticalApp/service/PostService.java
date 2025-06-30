package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.dto.PostDTO;
import com.jgy36.PoliticalApp.entity.*;
import com.jgy36.PoliticalApp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final HashtagRepository hashtagRepository;
    private final CommentRepository commentRepository;
    private final CommunityRepository communityRepository;
    private final PostLikeRepository postLikeRepository;
    private final NotificationService notificationService;
    @Autowired
    private LikeService likeService;

    public PostService(
            PostRepository postRepository,
            UserRepository userRepository,
            HashtagRepository hashtagRepository,
            CommentRepository commentRepository,
            CommunityRepository communityRepository,
            PostLikeRepository postLikeRepository,
            NotificationService notificationService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.hashtagRepository = hashtagRepository;
        this.commentRepository = commentRepository;
        this.communityRepository = communityRepository;
        this.postLikeRepository = postLikeRepository;
        this.notificationService = notificationService;
    }


    // ✅ Get all posts - Updated to use the new repository method that fetches original posts
    public List<PostDTO> getAllPosts() {
        // Use the new method that includes JOIN FETCH for original posts
        List<Post> posts = postRepository.findAllWithOriginalPostOrderByCreatedAtDesc();
        return posts.stream()
                .map(PostDTO::new)
                .collect(Collectors.toList());
    }

    // ✅ Get posts from users that the current user follows
    public List<PostDTO> getPostsFromFollowing(List<Long> followingIds) {
        List<Post> posts = postRepository.findPostsFromFollowing(followingIds);
        return posts.stream()
                .map(PostDTO::new)
                .collect(Collectors.toList());
    }

    // ✅ Create a new post
    @Transactional
    public Post createPost(String content) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Post post = new Post();
        post.setContent(content);
        post.setAuthor(user);
        post.setCreatedAt(LocalDateTime.now());

        // Extract and save hashtags
        Set<Hashtag> hashtags = extractHashtags(content);
        for (Hashtag hashtag : hashtags) {
            post.addHashtag(hashtag);
        }

        // Save the post first to get an ID
        Post savedPost = postRepository.save(post);

        // Detect mentions and create notifications
        Matcher mentionMatcher = Pattern.compile("@(\\w+(?:-\\w+)*)").matcher(content);
        while (mentionMatcher.find()) {
            String mentionedUsername = mentionMatcher.group(1);
            System.out.println("POST DEBUG: Found mention @" + mentionedUsername + " in post ID " + savedPost.getId());
            userRepository.findByUsername(mentionedUsername).ifPresent(mentionedUser -> {
                if (!mentionedUser.equals(user)) {
                    System.out.println("POST DEBUG: Creating notification for user " + mentionedUser.getUsername());
                    notificationService.createNotification(
                            mentionedUser,
                            user.getUsername() + " mentioned you in a post",
                            "mention",
                            savedPost.getId(),
                            null,
                            savedPost.getCommunity() != null ? savedPost.getCommunity().getSlug() : null
                    );
                } else {
                    System.out.println("POST DEBUG: Not creating notification for self-mention");
                }
            });
        }

        return savedPost;
    }

    // Method to extract hashtags from content
    private Set<Hashtag> extractHashtags(String content) {
        Set<Hashtag> hashtags = new HashSet<>();
        if (content == null || content.isEmpty()) {
            return hashtags;
        }

        Pattern pattern = Pattern.compile("#(\\w+)");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String tagText = "#" + matcher.group(1); // Include the # symbol
            // Find or create hashtag
            Hashtag hashtag = hashtagRepository.findByTag(tagText)
                    .orElseGet(() -> {
                        Hashtag newHashtag = new Hashtag(tagText);
                        return hashtagRepository.save(newHashtag);
                    });

            // If it's an existing hashtag, increment the count
            if (hashtag.getId() != null) {
                hashtag.setCount(hashtag.getCount() + 1);
                hashtagRepository.save(hashtag);
            }

            hashtags.add(hashtag);
        }

        return hashtags;
    }

    // ✅ Delete a post (only the author can delete their post)
    @Transactional
    public void deletePost(Long postId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User user = userOpt.get();
        Optional<Post> postOpt = postRepository.findById(postId);

        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found");
        }

        Post post = postOpt.get();

        if (!post.getAuthor().equals(user)) {
            throw new SecurityException("You are not allowed to delete this post.");
        }

        // Remove hashtag associations first (update hashtag counts)
        if (post.getHashtags() != null && !post.getHashtags().isEmpty()) {
            for (Hashtag hashtag : new HashSet<>(post.getHashtags())) {
                if (hashtag.getCount() > 0) {
                    hashtag.setCount(hashtag.getCount() - 1);
                    hashtagRepository.save(hashtag);
                }
                post.removeHashtag(hashtag);
            }
        }

        postRepository.delete(post);
    }

    // ✅ Like/Unlike a post
    @Transactional
    public int toggleLike(Long postId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found"));

        // Initialize collections if null
        if (post.getLikedUsers() == null) {
            post.setLikedUsers(new HashSet<>());
        }

        // Check if PostLike already exists
        Optional<PostLike> existingLike = postLikeRepository.findByPostAndUser(post, user);

        if (existingLike.isPresent()) {
            // Unlike: Remove the like
            postLikeRepository.delete(existingLike.get());
            post.getLikedUsers().remove(user);
        } else {
            // Like: Add the like
            PostLike postLike = new PostLike();
            postLike.setPost(post);
            postLike.setUser(user);
            postLikeRepository.save(postLike);

            // Add to likedUsers collection
            post.getLikedUsers().add(user);

            // Create notification for post author (if it's not the same user)
            if (!post.getAuthor().equals(user)) {
                notificationService.createNotification(
                        post.getAuthor(),
                        user.getUsername() + " liked your post",
                        "like",
                        post.getId(),
                        null,
                        post.getCommunity() != null ? post.getCommunity().getSlug() : null
                );
            }
        }

        // Save the post with updated likes
        Post savedPost = postRepository.save(post);

        // Return the updated like count
        return savedPost.getLikedUsers().size();
    }

    // ✅ Get users who liked a post
    public List<String> getPostLikes(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found with ID: " + postId));

        return post.getLikedUsers().stream()
                .map(User::getUsername)
                .collect(Collectors.toList());
    }

    // ✅ Save/Unsave a post
    @Transactional
    public void toggleSavePost(Long postId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email: " + email));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found with ID: " + postId));

        if (user.getSavedPosts() == null) {
            user.setSavedPosts(new HashSet<>());
        }

        if (user.getSavedPosts().contains(post)) {
            user.getSavedPosts().remove(post);
        } else {
            user.getSavedPosts().add(post);
        }

        userRepository.save(user);
    }

    // ✅ Get all saved posts for a user
    public List<Post> getSavedPosts(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email: " + email));

        if (user.getSavedPosts() == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(user.getSavedPosts());
    }

    // ✅ Get posts by hashtag
    public List<Post> getPostsByTag(String tag) {
        // Ensure tag has # prefix
        String normalizedTag = tag.startsWith("#") ? tag : "#" + tag;

        // Try to find hashtag entity
        Optional<Hashtag> hashtagOpt = hashtagRepository.findByTag(normalizedTag);

        if (hashtagOpt.isPresent()) {
            // If hashtag exists, return its posts
            return new ArrayList<>(hashtagOpt.get().getPosts());
        } else {
            // Otherwise search for posts containing the hashtag text
            return postRepository.findByContentContainingIgnoreCase(normalizedTag);
        }
    }

    // ✅ Get a post by ID
    public Post getPostById(Long postId) {
        return postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found with ID: " + postId));
    }

    // ✅ Find posts by a specific user
    // Update this method in PostService.java
    public List<PostDTO> getPostsByUserId(Long userId) {
        return postRepository.findByAuthorId(userId).stream()
                .map(PostDTO::new)
                .collect(Collectors.toList());
    }

    // ✅ Create a post in a specific community
    @Transactional
    public Post createCommunityPost(String communityId, String content) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Community community = communityRepository.findBySlug(communityId)
                .orElseThrow(() -> new IllegalArgumentException("Community not found"));

        // Check if user is a member of the community
        if (!community.isMember(user)) {
            throw new IllegalArgumentException("You must be a member of this community to post");
        }

        Post post = new Post();
        post.setContent(content);
        post.setAuthor(user);
        post.setCommunity(community);
        post.setCreatedAt(LocalDateTime.now());

        // Extract and save hashtags
        Set<Hashtag> hashtags = extractHashtags(content);
        for (Hashtag hashtag : hashtags) {
            post.addHashtag(hashtag);
        }

        return postRepository.save(post);
    }

    // Get posts by community slug
    public List<Post> getPostsByCommunitySlug(String slug) {
        return postRepository.findByCommunitySlugOrderByCreatedAtDesc(slug);
    }

    @Transactional(readOnly = true)
    public boolean isPostSavedByUser(Long postId, String email) {
        Optional<Post> postOpt = postRepository.findById(postId);
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (postOpt.isEmpty() || userOpt.isEmpty()) {
            return false;
        }

        Post post = postOpt.get();
        User user = userOpt.get();

        // Check if this post is in the user's saved posts collection
        if (user.getSavedPosts() != null) {
            return user.getSavedPosts().stream()
                    .anyMatch(savedPost -> savedPost.getId().equals(postId));
        }

        return false;
    }

    // Add these methods to your PostService class

    /**
     * Create a repost of an existing post
     */
    @Transactional
    public Post createRepost(String content, Long originalPostId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        // Find the original post
        Post originalPost = postRepository.findById(originalPostId)
                .orElseThrow(() -> new NoSuchElementException("Original post not found"));

        System.out.println("🔄 Creating repost - Original post found: " + originalPost.getId() +
                " by user " + originalPost.getAuthor().getUsername());

        // Create new post as a repost
        Post repost = new Post();
        repost.setContent(content);
        repost.setAuthor(user);
        repost.setCreatedAt(LocalDateTime.now());
        repost.setRepost(true);  // Explicitly set as repost
        repost.setOriginalPostId(originalPostId);
        repost.setOriginalPost(originalPost);  // Set the direct reference to original post

        // Optional: Copy hashtags from original post
        if (originalPost.getHashtags() != null && !originalPost.getHashtags().isEmpty()) {
            for (Hashtag hashtag : originalPost.getHashtags()) {
                repost.addHashtag(hashtag);
                // Update hashtag count
                hashtag.setCount(hashtag.getCount() + 1);
                hashtagRepository.save(hashtag);
            }
        }

        // Extract and save any new hashtags from the additional comment
        Set<Hashtag> newHashtags = extractHashtags(content);
        for (Hashtag hashtag : newHashtags) {
            if (!repost.getHashtags().contains(hashtag)) {
                repost.addHashtag(hashtag);
            }
        }

        System.out.println("🔄 Saving repost with explicit settings - isRepost: " + repost.isRepost() +
                ", originalPostId: " + repost.getOriginalPostId());

        // Save the repost
        Post savedRepost = postRepository.save(repost);

        // Increment the repost count on the original post
        originalPost.setRepostCount(originalPost.getRepostCount() + 1);
        postRepository.save(originalPost);

        // Create notification for the original post author (add this)
        User originalAuthor = originalPost.getAuthor();
        if (!originalAuthor.getId().equals(user.getId())) { // Don't notify if reposting own post
            String notificationMessage = user.getUsername() + " reposted your post: \"" +
                    (originalPost.getContent().length() > 30 ?
                            originalPost.getContent().substring(0, 30) + "..." :
                            originalPost.getContent()) + "\"";
            notificationService.createNotification(originalAuthor, notificationMessage);
        }

        System.out.println("🔄 Repost saved successfully - ID: " + savedRepost.getId() +
                ", isRepost: " + savedRepost.isRepost() +
                ", originalPostId: " + savedRepost.getOriginalPostId());

        return savedRepost;
    }

    /**
     * Get all reposts of a specific post
     */
    public List<Post> getRepostsOfPost(Long postId) {
        return postRepository.findRepostsOfPost(postId);
    }

    @Transactional
    public Post updatePost(Long postId, String content) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found"));

        // Check if the user is the author of the post
        if (!post.getAuthor().equals(user)) {
            throw new SecurityException("You are not allowed to edit this post.");
        }

        // Update the content
        post.setContent(content);

        // Set the updatedAt timestamp
        post.setUpdatedAt(LocalDateTime.now());

        // Extract and update hashtags
        Set<Hashtag> oldHashtags = new HashSet<>(post.getHashtags());

        // Remove old hashtags
        for (Hashtag hashtag : oldHashtags) {
            post.removeHashtag(hashtag);
            // Update hashtag count
            if (hashtag.getCount() > 0) {
                hashtag.setCount(hashtag.getCount() - 1);
                hashtagRepository.save(hashtag);
            }
        }

        // Extract and save new hashtags
        Set<Hashtag> newHashtags = extractHashtags(content);
        for (Hashtag hashtag : newHashtags) {
            post.addHashtag(hashtag);
        }

        // Save the updated post
        Post updatedPost = postRepository.save(post);

        // Detect mentions and create notifications
        Matcher mentionMatcher = Pattern.compile("@(\\w+(?:-\\w+)*)").matcher(content);
        while (mentionMatcher.find()) {
            String mentionedUsername = mentionMatcher.group(1);
            System.out.println("POST DEBUG: Found mention @" + mentionedUsername + " in updated post ID " + updatedPost.getId());
            userRepository.findByUsername(mentionedUsername).ifPresent(mentionedUser -> {
                if (!mentionedUser.equals(user)) {
                    System.out.println("POST DEBUG: Creating notification for user " + mentionedUser.getUsername());
                    notificationService.createNotification(
                            mentionedUser,
                            user.getUsername() + " mentioned you in an updated post",
                            "mention",
                            updatedPost.getId(),
                            null,
                            updatedPost.getCommunity() != null ? updatedPost.getCommunity().getSlug() : null
                    );
                } else {
                    System.out.println("POST DEBUG: Not creating notification for self-mention");
                }
            });
        }

        return updatedPost;
    }

    /**
     * Get posts from communities the user has joined
     */
    public List<PostDTO> getPostsFromUserCommunities(User user) {
        // Get communities the user is a member of
        List<Community> userCommunities = communityRepository.findCommunitiesByMember(user);

        if (userCommunities.isEmpty()) {
            return new ArrayList<>();
        }

        List<Post> posts = new ArrayList<>();

        // Collect posts from each community
        for (Community community : userCommunities) {
            posts.addAll(postRepository.findByCommunityOrderByCreatedAtDesc(community));
        }

        // Sort posts by creation time (most recent first)
        posts.sort(Comparator.comparing(Post::getCreatedAt).reversed());

        // Convert to DTOs
        return posts.stream()
                .map(post -> new PostDTO(post))
                .collect(Collectors.toList());
    }

    // Add this to PoliticalApp/src/main/java/com/jgy36/PoliticalApp/service/PostService.java
// Add this new method after the other createPost methods

    @Transactional
    public Post createPostWithMedia(
            String content,
            MultipartFile[] mediaFiles,
            String[] mediaTypes,
            String[] altTexts,
            Long originalPostId,
            boolean isRepost,
            Long communityId) throws IOException {

        // Create the post first
        Post post;
        if (isRepost && originalPostId != null) {
            post = createRepost(content, originalPostId);
        } else if (communityId != null) {
            post = createCommunityPost(communityId.toString(), content);
        } else {
            post = createPost(content);
        }

        // Save media files if provided
        if (mediaFiles != null && mediaFiles.length > 0) {
            try {
                File uploadDir = new File("uploads/media");
                if (!uploadDir.exists()) {
                    boolean created = uploadDir.mkdirs();
                    System.out.println("Created upload directory: " + created);
                }

                System.out.println("Upload directory path: " + uploadDir.getAbsolutePath());
                System.out.println("Upload directory exists: " + uploadDir.exists());
                System.out.println("Upload directory is writable: " + uploadDir.canWrite());

                for (int i = 0; i < mediaFiles.length; i++) {
                    MultipartFile file = mediaFiles[i];
                    if (file.isEmpty()) {
                        System.out.println("Skipping empty file at index " + i);
                        continue;
                    }

                    // Get media type
                    String mediaType = (mediaTypes != null && i < mediaTypes.length)
                            ? mediaTypes[i] : detectMediaType(file.getContentType());

                    // Get alt text
                    String altText = (altTexts != null && i < altTexts.length)
                            ? altTexts[i] : "";

                    // Generate filename with UUID to ensure uniqueness
                    String originalFilename = file.getOriginalFilename();
                    String filename = UUID.randomUUID().toString() + "_" +
                            (originalFilename != null ? originalFilename : "file");

                    // Make sure path exists
                    Path filePath = Paths.get(uploadDir.getAbsolutePath(), filename);

                    System.out.println("Saving file to: " + filePath.toString());

                    try {
                        // Save file to disk - using transferTo which is more reliable
                        file.transferTo(filePath.toFile());
                        System.out.println("File saved successfully to " + filePath);
                    } catch (IOException e) {
                        System.err.println("Error saving file: " + e.getMessage());
                        e.printStackTrace();
                        throw e;
                    }

                    // Create media attachment
                    MediaAttachment attachment = new MediaAttachment();
                    attachment.setPost(post);
                    attachment.setMediaType(mediaType);
                    attachment.setUrl("/media/" + filename);
                    attachment.setAltText(altText);

                    // Add the attachment to the post
                    post.getMediaAttachments().add(attachment);
                    System.out.println("Added attachment with URL: " + attachment.getUrl());
                }
            } catch (Exception e) {
                System.err.println("Error saving media files: " + e.getMessage());
                e.printStackTrace();
                throw new IOException("Failed to save media files", e);
            }
        }

        // Save the post with media attachments and flush to ensure immediate persistence
        Post savedPost = postRepository.saveAndFlush(post);
        System.out.println("Post saved with ID: " + savedPost.getId() +
                " and " + savedPost.getMediaAttachments().size() + " attachments");

        return savedPost;
    }

    // Helper method to detect media type
    private String detectMediaType(String contentType) {
        if (contentType == null) return "image";

        if (contentType.startsWith("image/")) {
            if (contentType.contains("gif")) {
                return "gif";
            }
            return "image";
        } else if (contentType.startsWith("video/")) {
            return "video";
        }

        return "image"; // Default
    }

}


