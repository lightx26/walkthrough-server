package com.pet.walkthroughserver.modules.walkthrough.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalkthroughCommentRepository extends JpaRepository<WalkthroughCommentEntity, UUID> {

    List<WalkthroughCommentEntity> findByWalkthroughIdOrderByCreatedAtAsc(UUID walkthroughId);

    List<WalkthroughCommentEntity> findByWalkthroughIdAndParentIdIsNullOrderByCreatedAtAsc(UUID walkthroughId);

    List<WalkthroughCommentEntity> findByWalkthroughFileIdAndParentIdIsNullOrderByCreatedAtAsc(UUID walkthroughFileId);

    List<WalkthroughCommentEntity> findByChapterIdAndWalkthroughFileIdIsNullAndParentIdIsNullOrderByCreatedAtAsc(UUID chapterId);

    List<WalkthroughCommentEntity> findByParentIdOrderByCreatedAtAsc(UUID parentId);

    Optional<WalkthroughCommentEntity> findByIdAndUserId(UUID id, UUID userId);
}
