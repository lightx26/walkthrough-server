package com.pet.walkthroughserver.modules.walkthrough.repository;

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
public class WalkthroughCommentEntity extends BaseEntity {

    @Column(name = "walkthrough_id", nullable = false)
    private UUID walkthroughId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "github_comment_id")
    private Long githubCommentId;

    @Builder.Default
    @Column(name = "sync_status", nullable = false, length = 20)
    private String syncStatus = "pending";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserEntity user;
}
