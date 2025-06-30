// Create a new file: PoliticalApp/src/main/java/com/jgy36/PoliticalApp/entity/MediaAttachment.java
package com.jgy36.PoliticalApp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "media_attachments")
public class MediaAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(nullable = false)
    private String mediaType; // "image", "video", "gif"

    @Column(nullable = false)
    private String url;

    @Column
    private String thumbnailUrl;

    @Column
    private String altText;

    // For images/GIFs
    @Column
    private Integer width;

    @Column
    private Integer height;

    // For videos
    @Column
    private Integer duration;
}
