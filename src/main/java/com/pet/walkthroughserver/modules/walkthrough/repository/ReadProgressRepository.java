package com.pet.walkthroughserver.modules.walkthrough.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReadProgressRepository extends JpaRepository<ReadProgressEntity, UUID> {

    Optional<ReadProgressEntity> findByUserIdAndWalkthroughId(UUID userId, UUID walkthroughId);

    List<ReadProgressEntity> findTop10ByUserIdOrderByReadAtDesc(UUID userId);

    @Query("""
            SELECT rp FROM ReadProgressEntity rp
            JOIN WalkthroughEntity w ON w.id = rp.walkthroughId
            WHERE rp.userId = :userId
              AND w.userId <> :userId
              AND w.status <> com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus.DRAFT
            ORDER BY rp.readAt DESC
            LIMIT 10
            """)
    List<ReadProgressEntity> findRecentlyReviewed(@Param("userId") UUID userId);
}
