package com.pet.walkthroughserver.modules.review.business.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.pet.walkthroughserver.modules.review.repository.ReviewDecision;
import com.pet.walkthroughserver.modules.review.repository.ReviewDecisionEntity;

public interface ReviewDecisionService {

    ReviewDecisionEntity upsertDecision(UUID userId, UUID walkthroughId, ReviewDecision decision, String comment);

    List<ReviewDecisionEntity> listDecisions(UUID walkthroughId);

    Optional<ReviewDecisionEntity> getMyDecision(UUID userId, UUID walkthroughId);

    void withdrawDecision(UUID userId, UUID walkthroughId);
}
