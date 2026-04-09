package com.pet.walkthroughserver.modules.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    List<CommentEntity> findByWalkthroughIdOrderByCreatedAtAsc(UUID walkthroughId);

    List<CommentEntity> findByWalkthroughIdAndParentIdIsNullOrderByCreatedAtAsc(UUID walkthroughId);

    List<CommentEntity> findByWalkthroughFileIdAndParentIdIsNullOrderByCreatedAtAsc(UUID walkthroughFileId);

    List<CommentEntity> findByChapterIdAndWalkthroughFileIdIsNullAndParentIdIsNullOrderByCreatedAtAsc(UUID chapterId);

    List<CommentEntity> findByParentIdOrderByCreatedAtAsc(UUID parentId);

    Optional<CommentEntity> findByIdAndUserId(UUID id, UUID userId);

    List<CommentEntity> findBySyncStatusAndRetryCountLessThan(String syncStatus, int maxRetries);
}
