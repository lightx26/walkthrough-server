package com.pet.walkthroughserver.modules.comment.business.events;

import com.pet.walkthroughserver.modules.comment.repository.CommentEntity;
import com.pet.walkthroughserver.modules.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Retries failed comment syncs up to {@code MAX_RETRIES} times.
 * After exhausting retries, marks the comment {@code permanently_failed}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentRetryScheduler {

    private static final int MAX_RETRIES = 3;

    private final CommentRepository commentRepository;
    private final CommentEventProducer commentEventProducer;

    @Scheduled(fixedDelay = 60_000)
    public void retryFailedComments() {
        List<CommentEntity> retryable =
                commentRepository.findBySyncStatusAndRetryCountLessThan("failed", MAX_RETRIES);

        if (retryable.isEmpty()) return;

        log.info("Retrying {} failed comment sync(s)", retryable.size());

        for (CommentEntity comment : retryable) {
            comment.setRetryCount(comment.getRetryCount() + 1);

            if (comment.getRetryCount() >= MAX_RETRIES) {
                comment.setSyncStatus("permanently_failed");
                log.warn("Comment {} permanently failed after {} retries", comment.getId(), MAX_RETRIES);
                commentRepository.save(comment);
                continue;
            }

            // Reset to pending so the consumer will re-process it
            comment.setSyncStatus("pending");
            commentRepository.save(comment);

            commentEventProducer.publish(CommentCreatedEvent.builder()
                    .commentId(comment.getId())
                    .walkthroughId(comment.getWalkthroughId())
                    .userId(comment.getUserId())
                    .content(comment.getContent())
                    .build());

            log.info("Requeued comment {} for retry (attempt {})", comment.getId(), comment.getRetryCount());
        }
    }
}
