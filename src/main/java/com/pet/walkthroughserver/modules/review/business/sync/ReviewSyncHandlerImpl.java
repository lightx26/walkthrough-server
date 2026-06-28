package com.pet.walkthroughserver.modules.review.business.sync;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.modules.review.business.services.GitHubReviewService;
import com.pet.walkthroughserver.modules.review.repository.ReviewDecision;
import com.pet.walkthroughserver.modules.review.repository.ReviewDecisionEntity;
import com.pet.walkthroughserver.modules.review.repository.ReviewDecisionRepository;
import com.pet.walkthroughserver.modules.review.repository.ReviewSyncStatus;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewSyncHandlerImpl implements ReviewSyncHandler {

    private final ReviewDecisionRepository reviewDecisionRepository;
    private final WalkthroughRepository walkthroughRepository;
    private final GitHubReviewService gitHubReviewService;
    private final UserService userService;

    @Override
    public void handle(ReviewSyncCommand command) {
        switch (command.action()) {
            case SUBMIT -> handleSubmit(command);
            case DISMISS -> handleDismiss(command);
        }
    }

    private void handleSubmit(ReviewSyncCommand command) {
        log.info("Processing review sync (SUBMIT) for decision {}", command.reviewDecisionId());

        Optional<ReviewDecisionEntity> decisionOpt = reviewDecisionRepository.findById(command.reviewDecisionId());
        if (decisionOpt.isEmpty()) {
            log.warn("Review decision {} not found, skipping sync", command.reviewDecisionId());
            return;
        }
        ReviewDecisionEntity decision = decisionOpt.get();

        Optional<WalkthroughEntity> walkthroughOpt = walkthroughRepository.findById(command.walkthroughId());
        if (walkthroughOpt.isEmpty()) {
            log.warn("Walkthrough {} not found, marking review decision {} as failed",
                    command.walkthroughId(), command.reviewDecisionId());
            markFailed(decision);
            return;
        }
        WalkthroughEntity walkthrough = walkthroughOpt.get();

        try {
            UserEntity user = userService.findById(command.userId());
            String event = toGitHubEvent(decision.getDecision());
            String body = formatBody(user.getUsername(), walkthrough.getTitle(), decision.getComment());

            Long reviewId = gitHubReviewService.submitReview(
                    command.userId(),
                    walkthrough.getOwner(),
                    walkthrough.getRepo(),
                    walkthrough.getPrNumber(),
                    body,
                    event
            );
            decision.setGithubReviewId(reviewId);
            decision.setSyncStatus(ReviewSyncStatus.SYNCED);
            reviewDecisionRepository.save(decision);
            log.info("Review decision {} synced as GitHub review {}", decision.getId(), reviewId);
        } catch (Exception e) {
            log.error("Failed to sync review decision {} to GitHub: {}", command.reviewDecisionId(), e.getMessage());
            markFailed(decision);
        }
    }

    private void handleDismiss(ReviewSyncCommand command) {
        if (command.githubReviewId() == null) {
            log.info("Withdrawn review for walkthrough {} was never synced to GitHub — nothing to dismiss",
                    command.walkthroughId());
            return;
        }
        log.info("Processing review sync (DISMISS) for GitHub review {}", command.githubReviewId());

        try {
            gitHubReviewService.dismissReview(
                    command.userId(),
                    command.owner(),
                    command.repo(),
                    command.prNumber(),
                    command.githubReviewId(),
                    "Review withdrawn via Walkthrough"
            );
            log.info("GitHub review {} dismissed", command.githubReviewId());
        } catch (Exception e) {
            log.error("Failed to dismiss GitHub review {}: {}", command.githubReviewId(), e.getMessage());
        }
    }

    private void markFailed(ReviewDecisionEntity decision) {
        decision.setSyncStatus(ReviewSyncStatus.FAILED);
        reviewDecisionRepository.save(decision);
    }

    private String toGitHubEvent(ReviewDecision decision) {
        return decision == ReviewDecision.APPROVED ? "APPROVE" : "REQUEST_CHANGES";
    }

    private String formatBody(String username, String walkthroughTitle, String comment) {
        String header = String.format("**[Walkthrough: %s]** review by %s", walkthroughTitle, username);
        if (comment == null || comment.isBlank()) {
            return header;
        }
        return header + "\n\n" + comment;
    }
}
