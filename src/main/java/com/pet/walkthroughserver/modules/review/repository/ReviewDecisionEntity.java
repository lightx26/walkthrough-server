package com.pet.walkthroughserver.modules.review.repository;

import java.util.UUID;

import com.pet.walkthroughserver.modules._shared.entity.BaseEntity;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "walkthrough_review_decisions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDecisionEntity extends BaseEntity {

    @Column(name = "walkthrough_id", nullable = false)
    private UUID walkthroughId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private ReviewDecision decision;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "github_review_id")
    private Long githubReviewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 20)
    private ReviewSyncStatus syncStatus;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserEntity user;
}
