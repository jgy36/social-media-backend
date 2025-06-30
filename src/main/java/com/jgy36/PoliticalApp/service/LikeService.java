package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.entity.*;
import com.jgy36.PoliticalApp.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class LikeService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public LikeService(
            PostRepository postRepository,
            CommentRepository commentRepository,
            PostLikeRepository postLikeRepository,
            CommentLikeRepository commentLikeRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    @Transactional
    public void likePost(Long postId) {
        User currentUser = getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found"));

        // Check if already liked
        if (postLikeRepository.findByPostAndUser(post, currentUser).isPresent()) {
            // Unlike the post
            postLikeRepository.deleteByPostAndUser(post, currentUser);
        } else {
            // Like the post
            PostLike postLike = new PostLike();
            postLike.setPost(post);
            postLike.setUser(currentUser);
            postLikeRepository.save(postLike);

            // Create notification if you're not liking your own post
            if (!post.getAuthor().equals(currentUser)) {
                notificationService.createNotification(
                        post.getAuthor(),
                        currentUser.getUsername() + " liked your post",
                        "like",
                        post.getId(),
                        null,
                        post.getCommunity() != null ? post.getCommunity().getSlug() : null
                );
            }
        }
    }

    @Transactional
    public void likeComment(Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("Comment not found"));

        // Check if already liked
        if (commentLikeRepository.existsByUserAndComment(currentUser, comment)) {
            // Unlike the comment
            commentLikeRepository.findByUserAndComment(currentUser, comment)
                    .ifPresent(commentLikeRepository::delete);
        } else {
            // Like the comment
            CommentLike commentLike = new CommentLike(currentUser, comment);
            commentLikeRepository.save(commentLike);

            // Create notification if you're not liking your own comment
            if (!comment.getUser().equals(currentUser)) {
                notificationService.createNotification(
                        comment.getUser(),
                        currentUser.getUsername() + " liked your comment",
                        "like",
                        comment.getPost().getId(),
                        comment.getId(),
                        comment.getPost().getCommunity() != null ? comment.getPost().getCommunity().getSlug() : null
                );
            }
        }
    }
}
