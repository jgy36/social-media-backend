package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.dto.CommunityPostRequest;
import com.jgy36.PoliticalApp.dto.PostDTO;
import com.jgy36.PoliticalApp.dto.PostRequest;
import com.jgy36.PoliticalApp.entity.MediaAttachment;
import com.jgy36.PoliticalApp.entity.Post;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.exception.ResourceNotFoundException;
import com.jgy36.PoliticalApp.repository.UserRepository;
import com.jgy36.PoliticalApp.service.PostService;
import com.jgy36.PoliticalApp.service.PrivacySettingsService;
import com.jgy36.PoliticalApp.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:3000") // FIX: Use specific origin
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final UserRepository userRepository;// ✅ Ensure this is declared
    private final UserService userService; // ✅ Ensure this is declared
    private final PrivacySettingsService privacySettingsService; // ✅ Ensure this is declared


    public PostController(PostService postService, UserRepository userRepository, UserService userService, PrivacySettingsService privacySettingsService) {
        this.postService = postService;
        this.userRepository = userRepository;
        this.userService = userService;
        this.privacySettingsService = privacySettingsService;
    }

    // ✅ Get all posts
    @GetMapping("/for-you")
    public ResponseEntity<List<PostDTO>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    // ✅ Get posts from users that the current user follows
    @GetMapping("/following")
    @PreAuthorize("isAuthenticated()") // ✅ Requires authentication
    public ResponseEntity<List<PostDTO>> getPostsFromFollowing() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 🔹 DEBUG: Print Authentication Details
        System.out.println("🔍 AUTH: " + auth);
        System.out.println("🔍 Authenticated? " + auth.isAuthenticated());
        System.out.println("🔍 Principal: " + auth.getPrincipal());

        if (auth == null || !auth.isAuthenticated()) {
            System.out.println("🚨 No valid authentication found! Returning 401.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            System.out.println("🚨 User not found in DB! Returning 401.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userOpt.get();
        List<Long> followingIds = user.getFollowing().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        if (followingIds.isEmpty()) {
            System.out.println("✅ User follows no one. Returning empty array.");
            return ResponseEntity.ok(List.of());
        }

        List<PostDTO> posts = postService.getPostsFromFollowing(followingIds);
        System.out.println("✅ Returning " + posts.size() + " posts from followed users.");
        return ResponseEntity.ok(posts);
    }

    // ✅ Create a new post
    // Replace your existing createPost method with this one:

    @PostMapping
    @PreAuthorize("isAuthenticated()") // Requires authentication
    public ResponseEntity<PostDTO> createPost(@RequestBody PostRequest request) {
        System.out.println("📝 Creating new post with content: " + request.getContent());
        System.out.println("🔄 IS_REPOST flag: " + request.isRepost());
        System.out.println("🔄 Original Post ID: " + request.getOriginalPostId());

        // Handle different types of posts
        Post createdPost;

        if (request.isRepost() && request.getOriginalPostId() != null) {
            // Handle repost
            System.out.println("🔄 Creating repost of post ID: " + request.getOriginalPostId());
            createdPost = postService.createRepost(request.getContent(), request.getOriginalPostId());

            // Add this debug code to verify the repost was created correctly
            System.out.println("🔄 Repost created with ID: " + createdPost.getId());
            System.out.println("🔄 Repost isRepost flag: " + createdPost.isRepost());
            System.out.println("🔄 Repost originalPostId: " + createdPost.getOriginalPostId());
            System.out.println("🔄 Repost originalPost loaded: " + (createdPost.getOriginalPost() != null));

            if (createdPost.getOriginalPost() != null) {
                System.out.println("🔄 Original post author: " +
                        (createdPost.getOriginalPost().getAuthor() != null ?
                                createdPost.getOriginalPost().getAuthor().getUsername() : "null"));
                System.out.println("🔄 Original post content: " + createdPost.getOriginalPost().getContent());
            }
        } else if (request.getCommunityId() != null) {
            // Handle community post
            System.out.println("👥 Creating community post in community ID: " + request.getCommunityId());
            createdPost = postService.createCommunityPost(request.getCommunityId().toString(), request.getContent());
        } else {
            // Handle normal post
            System.out.println("✅ Creating normal post");
            createdPost = postService.createPost(request.getContent());
        }

        // Convert to DTO before returning
        PostDTO postDTO = new PostDTO(createdPost);

        // Add debug logging for the DTO
        System.out.println("📤 Returning PostDTO with isRepost: " + postDTO.isRepost());
        System.out.println("📤 Returning PostDTO with originalPostId: " + postDTO.getOriginalPostId());
        System.out.println("📤 Returning PostDTO with originalAuthor: " + postDTO.getOriginalAuthor());
        System.out.println("📤 Returning PostDTO with originalPostContent: " +
                (postDTO.getOriginalPostContent() != null ? "present" : "null"));

        return ResponseEntity.ok(postDTO);
    }

    // Add a new endpoint to get reposts of a post
    @GetMapping("/{postId}/reposts")
    public ResponseEntity<List<PostDTO>> getPostReposts(@PathVariable Long postId) {
        List<Post> reposts = postService.getRepostsOfPost(postId);
        List<PostDTO> repostDTOs = reposts.stream()
                .map(post -> new PostDTO(post))
                .collect(Collectors.toList());
        return ResponseEntity.ok(repostDTOs);
    }

    @GetMapping("/extract-hashtags")
    public ResponseEntity<List<String>> extractHashtags(@RequestParam String text) {
        // Use your extraction logic
        Pattern pattern = Pattern.compile("#(\\w+)");
        Matcher matcher = pattern.matcher(text);

        List<String> hashtags = new ArrayList<>();
        while (matcher.find()) {
            hashtags.add(matcher.group());
        }

        return ResponseEntity.ok(hashtags);
    }

    @GetMapping
    public ResponseEntity<List<PostDTO>> getPosts(
            @RequestParam(required = false) String communitySlug) {
        if (communitySlug != null) {
            // Get posts for specific community using the slug
            List<Post> posts = postService.getPostsByCommunitySlug(communitySlug);
            List<PostDTO> postDTOs = posts.stream()
                    .map(post -> new PostDTO(post))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(postDTOs);
        } else {
            // Get all posts if no communitySlug is provided
            return getAllPosts();
        }
    }

    // ✅ NEW: Create a post in a community
    @PostMapping("/community")
    @PreAuthorize("isAuthenticated()") // Requires authentication
    public ResponseEntity<PostDTO> createCommunityPost(@RequestBody CommunityPostRequest request) {
        try {
            Post post = postService.createCommunityPost(request.getCommunityId(), request.getContent());
            return ResponseEntity.ok(new PostDTO(post));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // In PostController.java
    @GetMapping("/{postId}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable Long postId) {
        Post post = postService.getPostById(postId);
        PostDTO dto = new PostDTO(post);
        return ResponseEntity.ok(dto);
    }

    // ✅ Delete a post (Only author can delete their own post)
    @DeleteMapping("/{postId}")
    @PreAuthorize("isAuthenticated()") // Requires authentication
    public ResponseEntity<String> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.ok("Post deleted successfully");
    }

    // In PostController.java
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostDTO>> getUserPosts(@PathVariable Long userId) {
        // Get current user (if authenticated)
        User currentUser = null;
        try {
            currentUser = userService.getCurrentUser();
            System.out.println("DEBUG: Current user ID: " + currentUser.getId() +
                    " (type: " + currentUser.getId().getClass().getName() + ")");
            System.out.println("DEBUG: Request userId: " + userId +
                    " (type: " + userId.getClass().getName() + ")");
        } catch (Exception e) {
            System.out.println("DEBUG: Failed to get current user: " + e.getMessage());
            // User not authenticated
        }

        // Check if target user account is private
        boolean isPrivate = privacySettingsService.isAccountPrivate(userId);
        boolean isOwner = currentUser != null && currentUser.getId().equals(userId);
        boolean isFollowing = false;

        // Check following status if user is authenticated
        if (currentUser != null) {
            User targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            isFollowing = currentUser.getFollowing().contains(targetUser);
        }

        // If private and not owner and not following, return empty list
        if (isPrivate && !isOwner && !isFollowing) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Otherwise return posts as normal
        List<PostDTO> postDTOs = postService.getPostsByUserId(userId);
        return ResponseEntity.ok(postDTOs);
    }

    // ✅ Like/Unlike a post
    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, Integer>> likePost(@PathVariable Long postId, Authentication auth) {
        int likeCount = postService.toggleLike(postId, auth.getName());
        return ResponseEntity.ok(Map.of("likesCount", likeCount));
    }

    // ✅ Get users who liked a post
    @GetMapping("/{postId}/likes")
    public ResponseEntity<List<String>> getPostLikes(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getPostLikes(postId));
    }

    // ✅ Save/Unsave a post
    @PostMapping("/{postId}/save")
    public ResponseEntity<String> savePost(@PathVariable Long postId, Authentication auth) {
        postService.toggleSavePost(postId, auth.getName());
        return ResponseEntity.ok("Post saved successfully");
    }

    // ✅ Get all saved posts for a user
    @GetMapping("/saved")
    public ResponseEntity<List<PostDTO>> getSavedPosts(Authentication auth) {
        List<Post> posts = postService.getSavedPosts(auth.getName());
        List<PostDTO> postDTOs = posts.stream()
                .map(post -> new PostDTO(post))
                .collect(Collectors.toList());
        return ResponseEntity.ok(postDTOs);
    }

    @GetMapping("/{postId}/saved-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> checkSavedStatus(@PathVariable Long postId, Authentication auth) {
        try {
            String email = auth.getName();
            boolean isSaved = postService.isPostSavedByUser(postId, email);

            Map<String, Boolean> response = new HashMap<>();
            response.put("isSaved", isSaved);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("Error checking saved status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("isSaved", false));
        }
    }

    @PutMapping("/{postId}")
    @PreAuthorize("isAuthenticated()") // Requires authentication
    public ResponseEntity<PostDTO> updatePost(
            @PathVariable Long postId,
            @RequestBody Map<String, String> request) {

        String content = request.get("content");

        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Post updatedPost = postService.updatePost(postId, content);
            PostDTO dto = new PostDTO(updatedPost);
            return ResponseEntity.ok(dto);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // This would be added to PostController.java
    @GetMapping("/communities")
    @PreAuthorize("isAuthenticated()") // Requires authentication
    public ResponseEntity<List<PostDTO>> getPostsFromCommunities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            System.out.println("🚨 No valid authentication found! Returning 401.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            System.out.println("🚨 User not found in DB! Returning 401.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userOpt.get();
        List<PostDTO> posts = postService.getPostsFromUserCommunities(user);
        System.out.println("✅ Returning " + posts.size() + " posts from user communities.");
        return ResponseEntity.ok(posts);
    }

    // Add this to PoliticalApp/src/main/java/com/jgy36/PoliticalApp/controller/PostController.java
// Add this method alongside the other POST endpoints

    @PostMapping(value = "/with-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostDTO> createPostWithMedia(
            @RequestPart("content") String content,
            @RequestPart(value = "media", required = false) MultipartFile[] mediaFiles,
            @RequestParam(value = "mediaTypes", required = false) String[] mediaTypes,
            @RequestParam(value = "altTexts", required = false) String[] altTexts,
            @RequestParam(value = "originalPostId", required = false) String originalPostId,
            @RequestParam(value = "repost", required = false) String repost,
            @RequestParam(value = "communityId", required = false) String communityId) {

        // ADD THIS DEBUG BLOCK AT THE VERY BEGINNING
        System.out.println("🔍 =================================");
        System.out.println("🔍 POST /with-media received parameters:");
        System.out.println("🔍   content: '" + content + "'");
        System.out.println("🔍   communityId: '" + communityId + "'");
        System.out.println("🔍   communityId == null? " + (communityId == null));
        System.out.println("🔍   communityId isEmpty? " + (communityId != null && communityId.isEmpty()));
        System.out.println("🔍   mediaFiles count: " + (mediaFiles != null ? mediaFiles.length : 0));
        System.out.println("🔍 =================================");

        try {
            System.out.println("📝 Creating post with media");
            System.out.println("  Content: " + content);
            System.out.println("  Files: " + (mediaFiles != null ? mediaFiles.length : 0));

            // Print file details
            if (mediaFiles != null) {
                for (int i = 0; i < mediaFiles.length; i++) {
                    System.out.println("  File " + i + ": " + mediaFiles[i].getOriginalFilename() +
                            " (" + mediaFiles[i].getSize() + " bytes)");
                }
            }

            // Process other parameters
            Long origPostId = null;
            if (originalPostId != null && !originalPostId.isEmpty()) {
                origPostId = Long.parseLong(originalPostId);
            }

            boolean isRepost = "true".equalsIgnoreCase(repost);

            Long commId = null;
            if (communityId != null && !communityId.isEmpty()) {
                commId = Long.parseLong(communityId);
            }

            // Create post with media
            Post post = postService.createPostWithMedia(
                    content, mediaFiles, mediaTypes, altTexts,
                    origPostId, isRepost, commId
            );

            // Print media attachment details from the created post
            if (post.getMediaAttachments() != null) {
                System.out.println("Post created with " + post.getMediaAttachments().size() + " media attachments");
                for (MediaAttachment att : post.getMediaAttachments()) {
                    System.out.println("  Attachment URL: " + att.getUrl());
                    System.out.println("  Attachment type: " + att.getMediaType());
                }
            } else {
                System.out.println("Post created but media attachments set is null");
            }

            // Create DTO and return
            PostDTO dto = new PostDTO(post);

            // Check media in DTO
            if (dto.getMedia() != null) {
                System.out.println("DTO has " + dto.getMedia().size() + " media items");
                for (PostDTO.MediaDTO media : dto.getMedia()) {
                    System.out.println("  DTO media URL: " + media.getUrl());
                }
            } else {
                System.out.println("DTO media list is null");
            }

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            System.err.println("Error creating post with media: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/test-file-write")
    public ResponseEntity<String> testFileWrite() {
        String testContent = "This is a test file";
        String uploadDir = "uploads/media";
        String fileName = "test_file_" + System.currentTimeMillis() + ".txt";
        File directory = new File(uploadDir);
        StringBuilder response = new StringBuilder();

        response.append("File Write Test Results:\n");
        response.append("Current directory: ").append(new File(".").getAbsolutePath()).append("\n");
        response.append("Target directory: ").append(directory.getAbsolutePath()).append("\n");
        response.append("Directory exists: ").append(directory.exists()).append("\n");
        response.append("Directory writable: ").append(directory.canWrite()).append("\n");

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            response.append("Created directory: ").append(created).append("\n");
        }

        try {
            File testFile = new File(directory, fileName);
            Files.write(testFile.toPath(), testContent.getBytes());
            response.append("Test file created: ").append(testFile.getAbsolutePath()).append("\n");
            response.append("File exists: ").append(testFile.exists()).append("\n");
            response.append("File size: ").append(testFile.length()).append(" bytes\n");
            response.append("File content: ").append(new String(Files.readAllBytes(testFile.toPath()))).append("\n");
        } catch (Exception e) {
            response.append("Error writing test file: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }

        return ResponseEntity.ok(response.toString());
    }

    @GetMapping("/debug-image-access")
    public ResponseEntity<Map<String, Object>> debugImageAccess() {
        Map<String, Object> response = new HashMap<>();

        // Get directory info
        File mediaDir = new File("uploads/media");
        response.put("directory_exists", mediaDir.exists());
        response.put("directory_path", mediaDir.getAbsolutePath());

        // Get file list
        if (mediaDir.exists() && mediaDir.isDirectory()) {
            File[] files = mediaDir.listFiles();
            if (files != null && files.length > 0) {
                List<Map<String, Object>> fileInfos = new ArrayList<>();

                for (File file : files) {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("name", file.getName());
                    fileInfo.put("size", file.length());
                    fileInfo.put("url", "/media/" + file.getName());

                    fileInfos.add(fileInfo);
                }

                response.put("files", fileInfos);
                response.put("file_count", files.length);
            } else {
                response.put("files", Collections.emptyList());
                response.put("file_count", 0);
            }
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test-media-access")
    public ResponseEntity<Map<String, Object>> testMediaAccess() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Create test file with timestamp
            File uploadDir = new File("uploads/media");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String fileName = "test_image_" + System.currentTimeMillis() + ".txt";
            Path filePath = Paths.get(uploadDir.getAbsolutePath(), fileName);

            // Create a simple test file
            Files.write(filePath, "Test image content".getBytes());

            response.put("testFileCreated", true);
            response.put("testFileName", fileName);
            response.put("testFilePath", filePath.toString());
            response.put("testFileUrl", "/media/" + fileName);
            response.put("testFileDirectAccess", "http://localhost:8080/media/" + fileName);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


}
