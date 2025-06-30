package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.Post;
import com.jgy36.PoliticalApp.entity.PostLike;
import com.jgy36.PoliticalApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostAndUser(Post post, User user);

    void deleteByPostAndUser(Post post, User user);
}
