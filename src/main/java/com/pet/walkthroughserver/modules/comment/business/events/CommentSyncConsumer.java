package com.pet.walkthroughserver.modules.comment.business.events;

import java.util.Optional;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.configs.RabbitMQConfig;
import com.pet.walkthroughserver.modules.comment.business.services.GitHubCommentService;
import com.pet.walkthroughserver.modules.comment.business.util.DiffPositionParser;
import com.pet.walkthroughserver.modules.comment.repository.CommentEntity;
import com.pet.walkthroughserver.modules.comment.repository.CommentRepository;
import com.pet.walkthroughserver.modules.comment.repository.SyncStatus;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentSyncConsumer {

    private final CommentRepository commentRepository;
    private final WalkthroughRepository walkthroughRepository;
    private final WalkthroughFileRepository walkthroughFileRepository;
    private final GitHubCommentService gitHubCommentService;
    private final UserService userService;

    @RabbitListener(queues = RabbitMQConfig.COMMENT_QUEUE)
    public void handleCommentCreated(CommentCreatedEvent event) {
        log.info("Received CommentCreatedEvent for comment {}", event.getCommentId());

        Optional<CommentEntity> commentOpt = commentRepository.findById(event.getCommentId());
        if (commentOpt.isEmpty()) {
            log.warn("Comment {} not found, skipping sync", event.getCommentId());
            return;
        }
        CommentEntity comment = commentOpt.get();

        Optional<WalkthroughEntity> walkthroughOpt = walkthroughRepository.findById(event.getWalkthroughId());
        if (walkthroughOpt.isEmpty()) {
            log.warn("Walkthrough {} not found, marking comment {} as failed",
                    event.getWalkthroughId(), event.getCommentId());
            markFailed(comment);
            return;
        }
        WalkthroughEntity walkthrough = walkthroughOpt.get();

        try {
            UserEntity user = userService.findById(event.getUserId());
            syncComment(comment, walkthrough, user);
        } catch (Exception e) {
            log.error("Failed to sync comment {} to GitHub: {}", event.getCommentId(), e.getMessage());
            markFailed(comment);
        }
    }

    private void syncComment(CommentEntity comment,
                              WalkthroughEntity walkthrough,
                              UserEntity user) {
        // Line-level comment: use Pull Review Comment API
        if (comment.getWalkthroughFileId() != null && comment.getDiffPosition() != null) {
            syncLineLevelComment(comment, walkthrough, user);
            return;
        }

        // Chapter-level or walkthrough-level comment: use issue comment API
        syncGeneralComment(comment, walkthrough, user);
    }

    private void syncLineLevelComment(CommentEntity comment,
                                       WalkthroughEntity walkthrough,
                                       UserEntity user) {
        Optional<WalkthroughFileEntity> fileOpt =
                walkthroughFileRepository.findById(comment.getWalkthroughFileId());

        if (fileOpt.isEmpty()) {
            log.warn("File {} not found for line comment {} — cannot sync to specific line",
                    comment.getWalkthroughFileId(), comment.getId());
            markFailed(comment);
            return;
        }
        WalkthroughFileEntity file = fileOpt.get();

        // Validate the position exists in the stored patch
        if (file.getRawPatch() == null ||
                !DiffPositionParser.isValidPosition(file.getRawPatch(), comment.getDiffPosition())) {
            log.warn("diff_position {} is no longer valid in raw_patch for comment {} — marking FAILED (OUTDATED_DIFF)",
                    comment.getDiffPosition(), comment.getId());
            comment.setSyncStatus(SyncStatus.FAILED);
            commentRepository.save(comment);
            return;
        }

        // Require commit_sha to call the Pull Review Comment API
        if (walkthrough.getCommitSha() == null) {
            log.warn("Walkthrough {} has no commit_sha — cannot sync line comment without a commit anchor",
                    walkthrough.getId());
            markFailed(comment);
            return;
        }

        String body = formatBody(user.getUsername(), comment.getContent());

        try {
            // Post using the commenter's token so the GitHub comment is attributed
            // to the person who actually wrote it.
            Long githubCommentId = gitHubCommentService.createPrReviewComment(
                    comment.getUserId(),
                    walkthrough.getOwner(),
                    walkthrough.getRepo(),
                    walkthrough.getPrNumber(),
                    body,
                    walkthrough.getCommitSha(),
                    file.getFilename(),
                    comment.getDiffPosition()
            );
            comment.setGithubCommentId(githubCommentId);
            comment.setSyncStatus(SyncStatus.SYNCED);
            log.info("Line comment {} synced as review comment {}", comment.getId(), githubCommentId);
        } catch (Exception e) {
            log.error("Pull review comment sync failed for comment {}: {}", comment.getId(), e.getMessage());
            // If it failed due to an outdated commit, mark appropriately; otherwise re-throw
            if (e.getMessage() != null && e.getMessage().contains("422")) {
                comment.setSyncStatus(SyncStatus.FAILED);
                log.warn("Commit SHA may be outdated for comment {}", comment.getId());
            } else {
                throw e;
            }
        }
        commentRepository.save(comment);
    }

    private void syncGeneralComment(CommentEntity comment,
                                     WalkthroughEntity walkthrough,
                                     UserEntity user) {
        String body = String.format("**[Walkthrough: %s]** %s commented:\n\n%s",
                walkthrough.getTitle(), user.getUsername(), comment.getContent());

        // Post using the commenter's token so the GitHub comment is attributed
        // to the person who actually wrote it.
        Long githubCommentId = gitHubCommentService.createPrComment(
                comment.getUserId(),
                walkthrough.getOwner(),
                walkthrough.getRepo(),
                walkthrough.getPrNumber(),
                body
        );
        comment.setGithubCommentId(githubCommentId);
        comment.setSyncStatus(SyncStatus.SYNCED);
        log.info("General comment {} synced as issue comment {}", comment.getId(), githubCommentId);
        commentRepository.save(comment);
    }

    private void markFailed(CommentEntity comment) {
        comment.setSyncStatus(SyncStatus.FAILED);
        commentRepository.save(comment);
    }

    private String formatBody(String username, String content) {
        return String.format("**%s** commented via Walkthrough:\n\n%s", username, content);
    }
}
