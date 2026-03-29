package com.pet.walkthroughserver.modules.walkthrough.business.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules.github.business.services.GitHubService;
import com.pet.walkthroughserver.modules.walkthrough.business.events.CommentCreatedEvent;
import com.pet.walkthroughserver.modules.walkthrough.business.events.CommentEventProducer;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.CommentNotFoundException;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughAccessDeniedException;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughNotFoundException;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.AnnotationRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.ChapterRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.CreateCommentRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.CreateWalkthroughRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.UpdateWalkthroughRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.WalkthroughFileRequest;
import com.pet.walkthroughserver.modules.walkthrough.repository.AnnotationEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughCommentEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughCommentRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalkthroughServiceImpl implements WalkthroughService {

    private final WalkthroughRepository walkthroughRepository;
    private final WalkthroughCommentRepository commentRepository;
    private final GitHubService gitHubService;
    private final CommentEventProducer commentEventProducer;

    @Override
    @Transactional
    public WalkthroughEntity create(UUID userId, String username, CreateWalkthroughRequest request) {
        verifyPrOwnership(userId, username, request);

        WalkthroughEntity walkthrough = WalkthroughEntity.builder()
                .userId(userId)
                .owner(request.getOwner())
                .repo(request.getRepo())
                .prNumber(request.getPrNumber())
                .title(request.getTitle())
                .description(request.getDescription())
                .status("draft")
                .build();

        buildChapters(walkthrough, request.getChapters());
        return walkthroughRepository.save(walkthrough);
    }

    @Override
    public List<WalkthroughEntity> listByPr(String owner, String repo, Integer prNumber) {
        return walkthroughRepository.findByOwnerAndRepoAndPrNumberOrderByCreatedAtDesc(
                owner, repo, prNumber);
    }

    @Override
    public WalkthroughEntity getById(UUID id) {
        return walkthroughRepository.findById(id)
                .orElseThrow(() -> new WalkthroughNotFoundException("Walkthrough not found"));
    }

    @Override
    @Transactional
    public WalkthroughEntity update(UUID userId, UUID walkthroughId, UpdateWalkthroughRequest request) {
        WalkthroughEntity walkthrough = getById(walkthroughId);
        verifyOwnership(walkthrough, userId);

        walkthrough.setTitle(request.getTitle());
        walkthrough.setDescription(request.getDescription());
        walkthrough.setStatus(request.getStatus());
        walkthrough.getChapters().clear();
        buildChapters(walkthrough, request.getChapters());

        return walkthroughRepository.save(walkthrough);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID walkthroughId) {
        WalkthroughEntity walkthrough = getById(walkthroughId);
        verifyOwnership(walkthrough, userId);
        walkthroughRepository.delete(walkthrough);
    }

    // ── Comments ──

    @Override
    @Transactional
    public WalkthroughCommentEntity createComment(UUID userId, UUID walkthroughId, CreateCommentRequest request) {
        // Verify walkthrough exists
        getById(walkthroughId);

        WalkthroughCommentEntity comment = WalkthroughCommentEntity.builder()
                .walkthroughId(walkthroughId)
                .userId(userId)
                .content(request.getContent())
                .syncStatus("pending")
                .build();

        WalkthroughCommentEntity saved = commentRepository.save(comment);

        commentEventProducer.publish(CommentCreatedEvent.builder()
                .commentId(saved.getId())
                .walkthroughId(walkthroughId)
                .userId(userId)
                .content(request.getContent())
                .build());

        return saved;
    }

    @Override
    public List<WalkthroughCommentEntity> listComments(UUID walkthroughId) {
        return commentRepository.findByWalkthroughIdOrderByCreatedAtAsc(walkthroughId);
    }

    @Override
    @Transactional
    public void deleteComment(UUID userId, UUID commentId) {
        WalkthroughCommentEntity comment = commentRepository.findByIdAndUserId(commentId, userId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));
        commentRepository.delete(comment);
    }

    // ── Private helpers ──

    private void verifyOwnership(WalkthroughEntity walkthrough, UUID userId) {
        if (!walkthrough.getUserId().equals(userId)) {
            throw new WalkthroughAccessDeniedException("You do not own this walkthrough");
        }
    }

    private void verifyPrOwnership(UUID userId, String username, CreateWalkthroughRequest request) {
        GitHubPullRequest pr = gitHubService.getPullRequest(
                userId, request.getOwner(), request.getRepo(), request.getPrNumber());
        String prAuthorLogin = pr.getUser().getLogin();
        if (!username.equalsIgnoreCase(prAuthorLogin)) {
            throw new WalkthroughAccessDeniedException(
                    "Only the PR owner can create a walkthrough for this pull request");
        }
    }

    private void buildChapters(WalkthroughEntity walkthrough, List<ChapterRequest> chapterRequests) {
        for (int i = 0; i < chapterRequests.size(); i++) {
            ChapterRequest cr = chapterRequests.get(i);
            ChapterEntity chapter = ChapterEntity.builder()
                    .walkthrough(walkthrough)
                    .title(cr.getTitle())
                    .description(cr.getDescription())
                    .sortOrder(i)
                    .build();

            buildFiles(chapter, cr.getFiles());
            walkthrough.getChapters().add(chapter);
        }
    }

    private void buildFiles(ChapterEntity chapter, List<WalkthroughFileRequest> fileRequests) {
        for (int i = 0; i < fileRequests.size(); i++) {
            WalkthroughFileRequest fr = fileRequests.get(i);
            WalkthroughFileEntity file = WalkthroughFileEntity.builder()
                    .chapter(chapter)
                    .filename(fr.getFilename())
                    .fileSha(fr.getFileSha())
                    .fileStatus(fr.getFileStatus())
                    .sortOrder(i)
                    .build();

            buildAnnotations(file, fr.getAnnotations());
            chapter.getFiles().add(file);
        }
    }

    private void buildAnnotations(WalkthroughFileEntity file, List<AnnotationRequest> annotationRequests) {
        for (int i = 0; i < annotationRequests.size(); i++) {
            AnnotationRequest ar = annotationRequests.get(i);
            AnnotationEntity annotation = AnnotationEntity.builder()
                    .walkthroughFile(file)
                    .startLine(ar.getStartLine())
                    .endLine(ar.getEndLine())
                    .lineSide(ar.getLineSide())
                    .content(ar.getContent())
                    .sortOrder(i)
                    .build();
            file.getAnnotations().add(annotation);
        }
    }
}
