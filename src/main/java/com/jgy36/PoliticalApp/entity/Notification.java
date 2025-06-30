package com.jgy36.PoliticalApp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User recipient;

    @Column(nullable = false)
    private String message;

    private boolean read = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Add these new fields
    @Column(name = "notification_type")
    private String notificationType; // "post_created", "comment_created", "like", "follow", etc.

    @Column(name = "reference_id")
    private Long referenceId; // ID of the referenced entity (post, comment, user)

    @Column(name = "secondary_reference_id")
    private Long secondaryReferenceId; // For cases like "comment on post" where we need both IDs

    @Column(name = "community_id")
    private String communityId; // For community-related notifications
}
