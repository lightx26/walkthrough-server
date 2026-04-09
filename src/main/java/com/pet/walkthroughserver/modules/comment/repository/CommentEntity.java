package com.pet.walkthroughserver.modules.comment.repository;

import com.pet.walkthroughserver.modules._shared.entity.BaseEntity;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "walkthrough_comments")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CommentEntity extends BaseEntity {

    @Column(name = "walkthrough_id", nullable = false)
    private UUID walkthroughId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "chapter_id")
    private UUID chapterId;

    @Column(name = "walkthrough_file_id")
    private UUID walkthroughFileId;

    @Column(name = "diff_position")
    private Integer diffPosition;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "github_comment_id")
    private Long githubCommentId;

    @Builder.Default
    @Column(name = "sync_status", nullable = false, length = 20)
    private String syncStatus = "pending";

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserEntity user;
}
