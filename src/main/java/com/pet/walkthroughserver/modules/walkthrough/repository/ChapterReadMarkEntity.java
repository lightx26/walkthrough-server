package com.pet.walkthroughserver.modules.walkthrough.repository;

import java.time.Instant;
import java.util.UUID;

import com.pet.walkthroughserver.modules._shared.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "chapter_read_marks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "chapter_id"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterReadMarkEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "walkthrough_id", nullable = false)
    private UUID walkthroughId;

    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    @Builder.Default
    @Column(name = "marked_at", nullable = false)
    private Instant markedAt = Instant.now();
}
