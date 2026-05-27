package com.pet.walkthroughserver.modules.walkthrough.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChapterReadMarkRepository extends JpaRepository<ChapterReadMarkEntity, UUID> {

    Optional<ChapterReadMarkEntity> findByUserIdAndChapterId(UUID userId, UUID chapterId);

    boolean existsByUserIdAndChapterId(UUID userId, UUID chapterId);

    void deleteByUserIdAndChapterId(UUID userId, UUID chapterId);

    @Query("SELECT m.chapterId FROM ChapterReadMarkEntity m WHERE m.userId = :userId AND m.walkthroughId = :walkthroughId")
    List<UUID> findMarkedChapterIds(@Param("userId") UUID userId, @Param("walkthroughId") UUID walkthroughId);

    long countByUserIdAndWalkthroughId(UUID userId, UUID walkthroughId);
}
