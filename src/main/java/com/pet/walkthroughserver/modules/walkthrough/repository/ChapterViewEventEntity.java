package com.pet.walkthroughserver.modules.walkthrough.repository;

import java.time.Instant;
import java.util.UUID;

import com.pet.walkthroughserver.modules._shared.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "chapter_view_events")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterViewEventEntity extends BaseEntity {

    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Builder.Default
    @Column(name = "time_spent_sec", nullable = false)
    private Integer timeSpentSec = 0;

    @Builder.Default
    @Column(name = "viewed_at", nullable = false)
    private Instant viewedAt = Instant.now();
}
