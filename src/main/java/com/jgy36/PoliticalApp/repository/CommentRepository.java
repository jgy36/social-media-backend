package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.Comment;
import com.jgy36.PoliticalApp.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    // ✅ Find comments by post
    List<Comment> findByPost(Post post);

    List<Comment> findByPostId(Long postId);

    
}
