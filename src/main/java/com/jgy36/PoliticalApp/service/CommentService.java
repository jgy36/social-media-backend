package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.dto.CommentDTO;
import com.jgy36.PoliticalApp.dto.UserDTO;
import com.jgy36.PoliticalApp.entity.Comment;
import com.jgy36.PoliticalApp.entity.CommentLike;
import com.jgy36.PoliticalApp.entity.Post;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.repository.CommentLikeRepository;
import com.jgy36.PoliticalApp.repository.CommentRepository;
import com.jgy36.PoliticalApp.repository.PostRepository;
import com.jgy36.PoliticalApp.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final NotificationService notificationService;
    private final CommentLikeRepository commentLikeRepository;

    public CommentService(CommentRepository commentRepository, UserRepository userRepository, PostRepository postRepository,
                          NotificationService notificationService, CommentLikeRepository commentLikeRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.notificationService = notificationService;
        this.commentLikeRepository = commentLikeRepository;
    }

    // ✅ Fetch all comments for a given post
    @Transactional(readOnly = true)
    public List<CommentDTO> getCommentsByPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found with ID: " + postId));

        List<Comment> comments = commentRepository.findByPost(post);

        return comments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private CommentDTO convertToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setLikesCount(comment.getCommentLikes().size());

        // Set user info
        UserDTO userDTO = new UserDTO();
        userDTO.setId(comment.getUser().getId());
        userDTO.setUsername(comment.getUser().getUsername());
        userDTO.setDisplayName(comment.getUser().getDisplayName());
        userDTO.setProfileImageUrl(comment.getUser().getProfileImageUrl());
        dto.setUser(userDTO);

        // Check if current user has liked this comment
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            User currentUser = userRepository.findByEmail(auth.getName()).orElse(null);
            if (currentUser != null) {
                dto.setLikedByCurrentUser(commentLikeRepository.existsByUserAndComment(currentUser, comment));
            }
        }

        return dto;
    }

    // ✅ Add a new comment to a post
    @Transactional
    public CommentDTO addComment(Long postId, String content) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NoSuchElementException("User not found with email: " + auth.getName()));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found with ID: " + postId));

        Comment comment = new Comment(content, user, post);
        Comment savedComment = commentRepository.save(comment);

        // Notify post author if different from commenter
        if (!post.getAuthor().equals(user)) {
            notificationService.createNotification(
                    post.getAuthor(),
                    user.getUsername() + " commented on your post",
                    "comment_created",
                    post.getId(),
                    savedComment.getId(),
                    post.getCommunity() != null ? post.getCommunity().getSlug() : null
            );
        }

        // Notify users who previously commented
        commentRepository.findByPost(post).stream()
                .map(Comment::getUser)
                .distinct()
                .filter(commentUser -> !commentUser.equals(user) && !commentUser.equals(post.getAuthor()))
                .forEach(prevUser -> notificationService.createNotification(
                        prevUser,
                        user.getUsername() + " also commented on a post you interacted with",
                        "comment_created",
                        post.getId(),
                        savedComment.getId(),
                        post.getCommunity() != null ? post.getCommunity().getSlug() : null
                ));

        // Detect Mentions and Notify Users - IMPROVED REGEX FOR HYPHENATED USERNAMES
        Matcher matcher = Pattern.compile("@(\\w+(?:-\\w+)*)").matcher(content);
        while (matcher.find()) {
            String mentionedUsername = matcher.group(1);
            System.out.println("COMMENT DEBUG: Found mention @" + mentionedUsername + " in comment for post " + post.getId());
            userRepository.findByUsername(mentionedUsername).ifPresent(mentionedUser -> {
                if (!mentionedUser.equals(user)) {
                    System.out.println("COMMENT DEBUG: Creating notification for user " + mentionedUser.getUsername());
                    notificationService.createNotification(
                            mentionedUser,
                            user.getUsername() + " mentioned you in a comment",
                            "mention",
                            post.getId(),
                            savedComment.getId(),
                            post.getCommunity() != null ? post.getCommunity().getSlug() : null
                    );
                } else {
                    System.out.println("COMMENT DEBUG: Not creating notification for self-mention");
                }
            });
        }

        return convertToDTO(savedComment);
    }

    // ✅ Like a comment
    @Transactional
    public CommentDTO likeComment(Long commentId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("Comment not found"));

        boolean alreadyLiked = commentLikeRepository.existsByUserAndComment(user, comment);

        if (alreadyLiked) {
            // Unlike the comment by removing the existing like
            commentLikeRepository.findByUserAndComment(user, comment)
                    .ifPresent(commentLikeRepository::delete);
        } else {
            // Like the comment
            CommentLike like = new CommentLike(user, comment);
            commentLikeRepository.save(like);

            // ✅ Notify comment owner
            if (!comment.getUser().equals(user)) {
                notificationService.createNotification(
                        comment.getUser(),
                        user.getUsername() + " liked your comment",
                        "like",
                        comment.getPost().getId(),
                        comment.getId(),
                        comment.getPost().getCommunity() != null ? comment.getPost().getCommunity().getSlug() : null
                );
            }
        }

        return convertToDTO(comment);
    }

    // ✅ Reply to a comment
    @Transactional
    public CommentDTO replyToComment(Long parentCommentId, String content) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Comment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new NoSuchElementException("Parent comment not found"));

        Comment reply = new Comment(content, user, parentComment.getPost());
        reply.setParentComment(parentComment);
        Comment savedReply = commentRepository.save(reply);

        // ✅ Notify parent comment author
        if (!parentComment.getUser().equals(user)) {
            notificationService.createNotification(
                    parentComment.getUser(),
                    user.getUsername() + " replied to your comment",
                    "comment_reply",
                    parentComment.getPost().getId(),
                    savedReply.getId(),
                    parentComment.getPost().getCommunity() != null ? parentComment.getPost().getCommunity().getSlug() : null
            );
        }

        // ✅ Detect Mentions and Notify Users
        Matcher matcher = Pattern.compile("@(\\w+)").matcher(content);
        while (matcher.find()) {
            String mentionedUsername = matcher.group(1);
            userRepository.findByUsername(mentionedUsername).ifPresent(mentionedUser -> {
                if (!mentionedUser.equals(user)) {
                    notificationService.createNotification(
                            mentionedUser,
                            user.getUsername() + " mentioned you in a reply",
                            "mention",
                            parentComment.getPost().getId(),
                            savedReply.getId(),
                            parentComment.getPost().getCommunity() != null ? parentComment.getPost().getCommunity().getSlug() : null
                    );
                }
            });
        }

        return convertToDTO(savedReply);
    }

    // ✅ Delete a comment
    @Transactional
    public void deleteComment(Long commentId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("Comment not found"));

        // ✅ Only allow deletion if the user is the owner or an admin
        if (!comment.getUser().equals(user) && !user.getRole().equals("ROLE_ADMIN")) {
            throw new SecurityException("You are not allowed to delete this comment.");
        }

        commentRepository.delete(comment);
    }
}
