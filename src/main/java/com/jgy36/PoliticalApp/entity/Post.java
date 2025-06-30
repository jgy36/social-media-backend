package com.jgy36.PoliticalApp.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cascade;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@NoArgsConstructor
@Table(name = "posts")
public class Post {

    // Add to existing Post.java
    private Boolean showOnDatingProfile = false;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "email", "verificationToken", "following", "savedPosts", "hibernateLazyInitializer", "handler", "posts", "likedPosts", "comments"})
    private User author;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("post")
    private Set<PostLike> likes = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "posts_liked_users",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "liked_users_id")
    )
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    @JsonIgnoreProperties({"password", "email", "verificationToken", "following", "savedPosts", "hibernateLazyInitializer", "handler"})
    private Set<User> likedUsers = new HashSet<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"post", "hibernateLazyInitializer", "handler"})
    private Set<Comment> comments = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "post_hashtags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "hashtag_id")
    )
    @JsonManagedReference
    private Set<Hashtag> hashtags = new HashSet<>();

    // Add this to PoliticalApp/src/main/java/com/jgy36/PoliticalApp/entity/Post.java
// Add this right after the existing relationships (like comments, hashtags, etc.)

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("post")
    private Set<MediaAttachment> mediaAttachments = new HashSet<>();
    // Optional community relationship - if your app has communities
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", referencedColumnName = "id")
    @JsonIgnoreProperties({"posts", "members", "moderators", "hibernateLazyInitializer", "handler"})
    private Community community;
    @Column(nullable = false)
    @JsonProperty("isRepost")
    private boolean isRepost = false;
    @Column(name = "original_post_id")
    private Long originalPostId;
    @Column(nullable = false)
    private int repostCount = 0;
    // Add this relationship to get the original post (if this is a repost)
    // For the original post relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_post_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "author", "likes", "comments", "hashtags", "reposts"})
    private Post originalPost;
    // For reposts relationship
    @OneToMany(mappedBy = "originalPost")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "originalPost", "likes", "comments", "hashtags"})
    private Set<Post> reposts = new HashSet<>();
    @Column(nullable = true)
    private LocalDateTime updatedAt;

    public Post(String content, User author) {
        this.content = content;
        this.author = author;
        this.createdAt = LocalDateTime.now();
    }

    // Add getter and setter if they're not already handled by Lombok
    public Set<MediaAttachment> getMediaAttachments() {
        return mediaAttachments;
    }

    public void setMediaAttachments(Set<MediaAttachment> mediaAttachments) {
        this.mediaAttachments = mediaAttachments;
    }

    public int getLikeCount() {
        return likes.size();
    }

    /**
     * Adds a hashtag to this post
     */
    public void addHashtag(Hashtag hashtag) {
        this.hashtags.add(hashtag);
        // Only add this post to hashtag's posts if it's not already there
        if (!hashtag.getPosts().contains(this)) {
            hashtag.getPosts().add(this);
        }
    }

    /**
     * Removes a hashtag from this post
     */
    public void removeHashtag(Hashtag hashtag) {
        this.hashtags.remove(hashtag);
        // Only remove this post from hashtag if it contains it
        if (hashtag.getPosts().contains(this)) {
            hashtag.getPosts().remove(this);
        }
    }

    // Add a getter/setter for the new field:
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
