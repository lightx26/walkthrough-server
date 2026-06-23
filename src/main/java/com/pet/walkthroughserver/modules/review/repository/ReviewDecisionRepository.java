package com.pet.walkthroughserver.modules.review.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewDecisionRepository extends JpaRepository<ReviewDecisionEntity, UUID> {

    List<ReviewDecisionEntity> findByWalkthroughIdOrderByCreatedAtAsc(UUID walkthroughId);

    Optional<ReviewDecisionEntity> findByWalkthroughIdAndUserId(UUID walkthroughId, UUID userId);
}
