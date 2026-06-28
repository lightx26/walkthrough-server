package com.pet.walkthroughserver.modules.review.business.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.pet.walkthroughserver.modules._shared.messaging.DomainEventPublisher;
import com.pet.walkthroughserver.modules.review.business.events.ReviewDecisionSubmittedEvent;
import com.pet.walkthroughserver.modules.review.business.events.ReviewDecisionWithdrawnEvent;
import com.pet.walkthroughserver.modules.review.exceptions.ReviewDecisionNotFoundException;
import com.pet.walkthroughserver.modules.review.repository.ReviewDecision;
import com.pet.walkthroughserver.modules.review.repository.ReviewDecisionEntity;
import com.pet.walkthroughserver.modules.review.repository.ReviewDecisionRepository;
import com.pet.walkthroughserver.modules.review.repository.ReviewSyncStatus;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughNotFoundException;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewDecisionServiceImpl implements ReviewDecisionService {

    private final ReviewDecisionRepository reviewDecisionRepository;
    private final WalkthroughRepository walkthroughRepository;
    private final DomainEventPublisher eventPublisher;

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
        entity.setSyncStatus(ReviewSyncStatus.PENDING);
        ReviewDecisionEntity saved = reviewDecisionRepository.save(entity);

        ReviewDecisionSubmittedEvent event = new ReviewDecisionSubmittedEvent(
                saved.getId(), walkthroughId, userId, java.time.Instant.now());
        publishAfterCommit(event);

        return saved;
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

        Long githubReviewId = entity.getGithubReviewId();
        WalkthroughEntity walkthrough = walkthroughRepository.findById(walkthroughId).orElse(null);

        reviewDecisionRepository.delete(entity);

        if (walkthrough != null) {
            ReviewDecisionWithdrawnEvent event = new ReviewDecisionWithdrawnEvent(
                    walkthroughId, userId, githubReviewId,
                    walkthrough.getOwner(), walkthrough.getRepo(), walkthrough.getPrNumber(),
                    java.time.Instant.now());
            publishAfterCommit(event);
        }
    }

    private void publishAfterCommit(com.pet.walkthroughserver.modules._shared.messaging.DomainEvent event) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publish(event);
            }
        });
    }
}
