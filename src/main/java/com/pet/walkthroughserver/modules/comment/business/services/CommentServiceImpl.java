package com.pet.walkthroughserver.modules.comment.business.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pet.walkthroughserver.modules.comment.business.events.CommentCreatedEvent;
import com.pet.walkthroughserver.modules.comment.business.events.CommentEventProducer;
import com.pet.walkthroughserver.modules.comment.exceptions.CommentNotFoundException;
import com.pet.walkthroughserver.modules.comment.presentation.dto.CreateCommentRequest;
import com.pet.walkthroughserver.modules.comment.repository.CommentEntity;
import com.pet.walkthroughserver.modules.comment.repository.CommentRepository;
import com.pet.walkthroughserver.modules._shared.infra.github.GitHubResourceClient;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubAccessTokenNotFoundException;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughNotFoundException;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final WalkthroughRepository walkthroughRepository;
    private final CommentEventProducer commentEventProducer;
    private final GitHubResourceClient gitHubResourceClient;
    private final UserService userService;

    @Override
    @Transactional
    public CommentEntity createComment(UUID userId, UUID walkthroughId, CreateCommentRequest request) {
        walkthroughRepository.findById(walkthroughId)
                .orElseThrow(() -> new WalkthroughNotFoundException("Walkthrough not found"));

        CommentEntity comment = CommentEntity.builder()
                .walkthroughId(walkthroughId)
                .userId(userId)
                .content(request.getContent())
                .chapterId(request.getChapterId())
                .walkthroughFileId(request.getWalkthroughFileId())
                .diffPosition(request.getDiffPosition())
                .parentId(request.getParentId())
                .syncStatus("pending")
                .build();

        CommentEntity saved = commentRepository.save(comment);

        commentEventProducer.publish(CommentCreatedEvent.builder()
                .commentId(saved.getId())
                .walkthroughId(walkthroughId)
                .userId(userId)
                .content(request.getContent())
                .walkthroughFileId(request.getWalkthroughFileId())
                .diffPosition(request.getDiffPosition())
                .build());

        return saved;
    }

    @Override
    public List<CommentEntity> listComments(UUID walkthroughId) {
        return commentRepository.findByWalkthroughIdAndParentIdIsNullOrderByCreatedAtAsc(walkthroughId);
    }

    @Override
    public List<CommentEntity> listFileComments(UUID walkthroughFileId) {
        return commentRepository.findByWalkthroughFileIdAndParentIdIsNullOrderByCreatedAtAsc(walkthroughFileId);
    }

    @Override
    public List<CommentEntity> listChapterComments(UUID chapterId) {
        return commentRepository.findByChapterIdAndWalkthroughFileIdIsNullAndParentIdIsNullOrderByCreatedAtAsc(chapterId);
    }

    @Override
    public List<CommentEntity> listReplies(UUID parentId) {
        return commentRepository.findByParentIdOrderByCreatedAtAsc(parentId);
    }

    @Override
    @Transactional
    public void deleteComment(UUID userId, UUID commentId) {
        CommentEntity comment = commentRepository.findByIdAndUserId(commentId, userId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));
        commentRepository.delete(comment);
    }

    @Override
    public Long createPrComment(UUID userId, String owner, String repo, int prNumber, String body) {
        String accessToken = getGitHubAccessToken(userId);
        return gitHubResourceClient.createIssueComment(accessToken, owner, repo, prNumber, body);
    }

    @Override
    public Long createPrReviewComment(UUID userId, String owner, String repo, int prNumber,
                                       String body, String commitId, String path, int position) {
        String accessToken = getGitHubAccessToken(userId);
        return gitHubResourceClient.createPullReviewComment(accessToken, owner, repo, prNumber, body, commitId, path, position);
    }

    private String getGitHubAccessToken(UUID userId) {
        UserEntity user = userService.findById(userId);
        String token = user.getGithubAccessToken();
        if (token == null || token.isBlank()) {
            throw new GitHubAccessTokenNotFoundException("GitHub access token not found. Please re-authenticate.");
        }
        return token;
    }
}
