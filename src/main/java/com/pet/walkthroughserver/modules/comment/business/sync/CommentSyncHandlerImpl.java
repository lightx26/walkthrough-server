package com.pet.walkthroughserver.modules.comment.business.sync;

import java.util.Optional;

import org.springframework.stereotype.Component;

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
public class CommentSyncHandlerImpl implements CommentSyncHandler {

    private final CommentRepository commentRepository;
    private final WalkthroughRepository walkthroughRepository;
    private final WalkthroughFileRepository walkthroughFileRepository;
    private final GitHubCommentService gitHubCommentService;
    private final UserService userService;

    @Override
    public void handle(CommentSyncCommand command) {
        log.info("Processing comment sync for comment {}", command.commentId());

        Optional<CommentEntity> commentOpt = commentRepository.findById(command.commentId());
        if (commentOpt.isEmpty()) {
            log.warn("Comment {} not found, skipping sync", command.commentId());
            return;
        }
        CommentEntity comment = commentOpt.get();

        Optional<WalkthroughEntity> walkthroughOpt = walkthroughRepository.findById(command.walkthroughId());
        if (walkthroughOpt.isEmpty()) {
            log.warn("Walkthrough {} not found, marking comment {} as failed",
                    command.walkthroughId(), command.commentId());
            markFailed(comment);
            return;
        }
        WalkthroughEntity walkthrough = walkthroughOpt.get();

        try {
            UserEntity user = userService.findById(command.userId());
            syncComment(comment, walkthrough, user);
        } catch (Exception e) {
            log.error("Failed to sync comment {} to GitHub: {}", command.commentId(), e.getMessage());
            markFailed(comment);
        }
    }

    private void syncComment(CommentEntity comment,
                              WalkthroughEntity walkthrough,
                              UserEntity user) {
        if (comment.getWalkthroughFileId() != null && comment.getDiffPosition() != null) {
            syncLineLevelComment(comment, walkthrough, user);
            return;
        }
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

        if (file.getRawPatch() == null ||
                !DiffPositionParser.isValidPosition(file.getRawPatch(), comment.getDiffPosition())) {
            log.warn("diff_position {} is no longer valid in raw_patch for comment {} — marking FAILED (OUTDATED_DIFF)",
                    comment.getDiffPosition(), comment.getId());
            comment.setSyncStatus(SyncStatus.FAILED);
            commentRepository.save(comment);
            return;
        }

        if (walkthrough.getCommitSha() == null) {
            log.warn("Walkthrough {} has no commit_sha — cannot sync line comment without a commit anchor",
                    walkthrough.getId());
            markFailed(comment);
            return;
        }

        String body = formatBody(user.getUsername(), comment.getContent());

        try {
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
