package com.pet.walkthroughserver.modules.review.business.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pet.walkthroughserver.modules.review.exceptions.ReviewDecisionNotFoundException;
import com.pet.walkthroughserver.modules.review.repository.ReviewDecision;
import com.pet.walkthroughserver.modules.review.repository.ReviewDecisionEntity;
import com.pet.walkthroughserver.modules.review.repository.ReviewDecisionRepository;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughNotFoundException;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewDecisionServiceImpl implements ReviewDecisionService {

    private final ReviewDecisionRepository reviewDecisionRepository;
    private final WalkthroughRepository walkthroughRepository;

    @Override
    @Transactional
    public ReviewDecisionEntity upsertDecision(UUID userId, UUID walkthroughId, ReviewDecision decision, String comment) {
        walkthroughRepository.findById(walkthroughId)
                .orElseThrow(() -> new WalkthroughNotFoundException("Walkthrough not found"));

        ReviewDecisionEntity entity = reviewDecisionRepository
                .findByWalkthroughIdAndUserId(walkthroughId, userId)
                .orElseGet(() -> ReviewDecisionEntity.builder()
                        .walkthroughId(walkthroughId)
                        .userId(userId)
                        .build());

        entity.setDecision(decision);
        entity.setComment(comment);
        return reviewDecisionRepository.save(entity);
    }

    @Override
    public List<ReviewDecisionEntity> listDecisions(UUID walkthroughId) {
        return reviewDecisionRepository.findByWalkthroughIdOrderByCreatedAtAsc(walkthroughId);
    }

    @Override
    public Optional<ReviewDecisionEntity> getMyDecision(UUID userId, UUID walkthroughId) {
        return reviewDecisionRepository.findByWalkthroughIdAndUserId(walkthroughId, userId);
    }

    @Override
    @Transactional
    public void withdrawDecision(UUID userId, UUID walkthroughId) {
        ReviewDecisionEntity entity = reviewDecisionRepository
                .findByWalkthroughIdAndUserId(walkthroughId, userId)
                .orElseThrow(() -> new ReviewDecisionNotFoundException("Review decision not found"));
        reviewDecisionRepository.delete(entity);
    }
}
