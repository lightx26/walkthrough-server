package com.pet.walkthroughserver.modules.walkthrough.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChapterViewEventRepository extends JpaRepository<ChapterViewEventEntity, UUID> {

    boolean existsByChapterIdAndUserId(UUID chapterId, UUID userId);
}
