package com.pet.walkthroughserver.modules.walkthrough.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WalkthroughRepository extends JpaRepository<WalkthroughEntity, UUID> {

    @Query("SELECT w FROM WalkthroughEntity w LEFT JOIN FETCH w.user WHERE w.id = :id")
    Optional<WalkthroughEntity> findByIdWithUser(@Param("id") UUID id);

    List<WalkthroughEntity> findByOwnerAndRepoAndPrNumberOrderByCreatedAtDesc(
            String owner, String repo, Integer prNumber);

    List<WalkthroughEntity> findTop10ByUserIdOrderByUpdatedAtDesc(UUID userId);

    List<WalkthroughEntity> findTop10ByStatusOrderByUpdatedAtDesc(WalkthroughStatus status);

    List<WalkthroughEntity> findByUserIdAndStatusOrderByUpdatedAtDesc(UUID userId, WalkthroughStatus status);

    List<WalkthroughEntity> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    List<WalkthroughEntity> findByUserIdAndOwnerAndRepoOrderByUpdatedAtDesc(UUID userId, String owner, String repo);

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

    @Query("SELECT COUNT(w) FROM WalkthroughEntity w " +
            "WHERE w.owner = :owner AND w.repo = :repo " +
            "AND (w.status <> com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus.DRAFT " +
            "OR w.userId = :userId)")
    long countByOwnerAndRepoForUser(@Param("owner") String owner, @Param("repo") String repo,
                                    @Param("userId") UUID userId);

    @Query("SELECT COUNT(w) FROM WalkthroughEntity w " +
            "WHERE w.owner = :owner AND w.repo = :repo AND w.prNumber = :prNumber " +
            "AND (w.status <> com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus.DRAFT " +
            "OR w.userId = :userId)")
    long countByOwnerAndRepoAndPrNumberForUser(@Param("owner") String owner, @Param("repo") String repo,
                                               @Param("prNumber") Integer prNumber, @Param("userId") UUID userId);

    @Query("SELECT CONCAT(w.owner, '/', w.repo) AS fullName, COUNT(w) " +
            "FROM WalkthroughEntity w WHERE CONCAT(w.owner, '/', w.repo) IN :fullNames " +
            "AND (w.status <> com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus.DRAFT " +
            "OR w.userId = :userId) " +
            "GROUP BY w.owner, w.repo")
    List<Object[]> countByRepoFullNamesForUser(@Param("fullNames") List<String> fullNames,
                                               @Param("userId") UUID userId);

    @Query("SELECT w.prNumber, COUNT(w) FROM WalkthroughEntity w " +
            "WHERE w.owner = :owner AND w.repo = :repo AND w.prNumber IN :prNumbers " +
            "AND (w.status <> com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus.DRAFT " +
            "OR w.userId = :userId) " +
            "GROUP BY w.prNumber")
    List<Object[]> countByPrNumbersForUser(@Param("owner") String owner, @Param("repo") String repo,
                                           @Param("prNumbers") List<Integer> prNumbers, @Param("userId") UUID userId);

    List<WalkthroughEntity> findByOwnerAndRepoAndPrNumberAndStatus(
            String owner, String repo, Integer prNumber, WalkthroughStatus status);
}
