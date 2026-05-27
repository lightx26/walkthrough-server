package com.pet.walkthroughserver.modules.walkthrough.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChapterViewEventRepository extends JpaRepository<ChapterViewEventEntity, UUID> {

    boolean existsByChapterIdAndUserId(UUID chapterId, UUID userId);

    @Query("SELECT DISTINCT e.chapterId FROM ChapterViewEventEntity e WHERE e.userId = :userId AND e.chapterId IN :chapterIds")
    List<UUID> findReadChapterIdsByUserIdAndChapterIds(@Param("userId") UUID userId, @Param("chapterIds") List<UUID> chapterIds);

    void deleteByChapterIdAndUserId(UUID chapterId, UUID userId);
}
