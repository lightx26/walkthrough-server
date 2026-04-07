package com.pet.walkthroughserver.modules.walkthrough.repository;

import com.pet.walkthroughserver.modules._shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "read_progress")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReadProgressEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "walkthrough_id", nullable = false)
    private UUID walkthroughId;

    @Column(name = "last_chapter_id")
    private UUID lastChapterId;

    @Builder.Default
    @Column(name = "read_chapters", nullable = false)
    private Integer readChapters = 0;

    @Builder.Default
    @Column(name = "total_chapters", nullable = false)
    private Integer totalChapters = 0;

    @Builder.Default
    @Column(name = "time_spent_sec", nullable = false)
    private Integer timeSpentSec = 0;

    @Builder.Default
    @Column(name = "read_at", nullable = false)
    private Instant readAt = Instant.now();
}
