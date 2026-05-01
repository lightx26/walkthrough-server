package com.pet.walkthroughserver.modules.walkthrough.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WalkthroughRepository extends JpaRepository<WalkthroughEntity, UUID> {

    List<WalkthroughEntity> findByOwnerAndRepoAndPrNumberOrderByCreatedAtDesc(
            String owner, String repo, Integer prNumber);

    List<WalkthroughEntity> findTop10ByUserIdOrderByUpdatedAtDesc(UUID userId);

    List<WalkthroughEntity> findTop10ByStatusOrderByUpdatedAtDesc(WalkthroughStatus status);

    List<WalkthroughEntity> findByUserIdAndStatusOrderByUpdatedAtDesc(UUID userId, WalkthroughStatus status);

    List<WalkthroughEntity> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, WalkthroughStatus status);

    @Query("SELECT COUNT(c) FROM ChapterEntity c JOIN c.walkthrough w WHERE w.userId = :userId")
    long countChaptersByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(cv) FROM ChapterViewEventEntity cv, ChapterEntity c " +
            "WHERE cv.chapterId = c.id AND c.walkthrough.userId = :userId")
    long countViewsByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(cm) FROM CommentEntity cm, WalkthroughEntity w " +
            "WHERE cm.walkthroughId = w.id AND w.userId = :userId")
    long countCommentsByUserId(@Param("userId") UUID userId);

    long countByOwnerAndRepo(String owner, String repo);

    List<WalkthroughEntity> findByOwnerAndRepoAndPrNumberAndStatus(
            String owner, String repo, Integer prNumber, WalkthroughStatus status);
}
