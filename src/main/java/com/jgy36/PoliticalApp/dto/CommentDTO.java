package com.jgy36.PoliticalApp.dto;

import java.time.LocalDateTime;

public class CommentDTO {
    private Long id;
    private UserDTO user;
    private String content;
    private LocalDateTime createdAt;
    private int likesCount;
    private boolean likedByCurrentUser;

    // Constructors
    public CommentDTO() {
    }

    public CommentDTO(Long id, UserDTO user, String content, LocalDateTime createdAt, int likesCount, boolean likedByCurrentUser) {
        this.id = id;
        this.user = user;
        this.content = content;
        this.createdAt = createdAt;
        this.likesCount = likesCount;
        this.likedByCurrentUser = likedByCurrentUser;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    public boolean isLikedByCurrentUser() {
        return likedByCurrentUser;
    }

    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        this.likedByCurrentUser = likedByCurrentUser;
    }
}
