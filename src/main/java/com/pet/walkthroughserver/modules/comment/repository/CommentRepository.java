package com.pet.walkthroughserver.modules.comment.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    List<CommentEntity> findByWalkthroughIdOrderByCreatedAtAsc(UUID walkthroughId);

    List<CommentEntity> findByWalkthroughIdAndParentIdIsNullOrderByCreatedAtAsc(UUID walkthroughId);

    List<CommentEntity> findByWalkthroughFileIdAndParentIdIsNullOrderByCreatedAtAsc(UUID walkthroughFileId);

    List<CommentEntity> findByWalkthroughFileIdInAndParentIdIsNullOrderByCreatedAtAsc(List<UUID> walkthroughFileIds);

    List<CommentEntity> findByChapterIdAndWalkthroughFileIdIsNullAndParentIdIsNullOrderByCreatedAtAsc(UUID chapterId);

    List<CommentEntity> findByParentIdOrderByCreatedAtAsc(UUID parentId);

    Optional<CommentEntity> findByIdAndUserId(UUID id, UUID userId);

    List<CommentEntity> findBySyncStatusAndRetryCountLessThan(SyncStatus syncStatus, int maxRetries);

    long countByWalkthroughId(UUID walkthroughId);

    @org.springframework.data.jpa.repository.Query(
            "SELECT c.walkthroughId, COUNT(c) FROM CommentEntity c WHERE c.walkthroughId IN :ids GROUP BY c.walkthroughId")
    List<Object[]> countGroupedByWalkthroughIds(@org.springframework.data.repository.query.Param("ids") List<UUID> ids);
}
