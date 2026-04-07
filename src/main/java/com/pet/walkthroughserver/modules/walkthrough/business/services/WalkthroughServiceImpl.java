package com.pet.walkthroughserver.modules.walkthrough.business.services;

import java.time.Instant;
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
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.RecordChapterViewRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.UpdateWalkthroughRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.WalkthroughFileRequest;
import com.pet.walkthroughserver.modules.walkthrough.repository.AnnotationEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterViewEventEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterViewEventRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.ReadProgressEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ReadProgressRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughCommentEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughCommentRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalkthroughServiceImpl implements WalkthroughService {

    private final WalkthroughRepository walkthroughRepository;
    private final WalkthroughCommentRepository commentRepository;
    private final ChapterViewEventRepository chapterViewEventRepository;
    private final ReadProgressRepository readProgressRepository;
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
                .status(request.getStatus())
                .build();

        buildChapters(walkthrough, request.getChapters());
        return walkthroughRepository.save(walkthrough);
    }

    @Override
    public List<WalkthroughEntity> listByPr(String owner, String repo, Integer prNumber, UUID requestingUserId) {
        return walkthroughRepository.findByOwnerAndRepoAndPrNumberOrderByCreatedAtDesc(owner, repo, prNumber)
                .stream()
                .filter(wt -> wt.getStatus() == WalkthroughStatus.PUBLISHED || wt.getUserId().equals(requestingUserId))
                .toList();
    }

    @Override
    public List<WalkthroughEntity> listRecent(UUID userId) {
        return walkthroughRepository.findTop10ByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Override
    public WalkthroughEntity getById(UUID id, UUID requestingUserId) {
        WalkthroughEntity walkthrough = walkthroughRepository.findById(id)
                .orElseThrow(() -> new WalkthroughNotFoundException("Walkthrough not found"));
        if (walkthrough.getStatus() == WalkthroughStatus.DRAFT && !walkthrough.getUserId().equals(requestingUserId)) {
            throw new WalkthroughNotFoundException("Walkthrough not found");
        }
        return walkthrough;
    }

    @Override
    @Transactional
    public WalkthroughEntity update(UUID userId, UUID walkthroughId, UpdateWalkthroughRequest request) {
        WalkthroughEntity walkthrough = findWalkthroughById(walkthroughId);
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
        WalkthroughEntity walkthrough = findWalkthroughById(walkthroughId);
        verifyOwnership(walkthrough, userId);
        walkthroughRepository.delete(walkthrough);
    }

    // ── Comments ──

    @Override
    @Transactional
    public WalkthroughCommentEntity createComment(UUID userId, UUID walkthroughId, CreateCommentRequest request) {
        findWalkthroughById(walkthroughId);

        WalkthroughCommentEntity comment = WalkthroughCommentEntity.builder()
                .walkthroughId(walkthroughId)
                .userId(userId)
                .content(request.getContent())
                .chapterId(request.getChapterId())
                .walkthroughFileId(request.getWalkthroughFileId())
                .diffPosition(request.getDiffPosition())
                .parentId(request.getParentId())
                .syncStatus("pending")
                .build();

        WalkthroughCommentEntity saved = commentRepository.save(comment);

        // Only publish sync event for line-level comments (those with a file + position)
        if (request.getWalkthroughFileId() != null && request.getDiffPosition() != null) {
            commentEventProducer.publish(CommentCreatedEvent.builder()
                    .commentId(saved.getId())
                    .walkthroughId(walkthroughId)
                    .userId(userId)
                    .content(request.getContent())
                    .build());
        }

        return saved;
    }

    @Override
    public List<WalkthroughCommentEntity> listComments(UUID walkthroughId) {
        return commentRepository.findByWalkthroughIdAndParentIdIsNullOrderByCreatedAtAsc(walkthroughId);
    }

    @Override
    public List<WalkthroughCommentEntity> listFileComments(UUID walkthroughFileId) {
        return commentRepository.findByWalkthroughFileIdAndParentIdIsNullOrderByCreatedAtAsc(walkthroughFileId);
    }

    @Override
    public List<WalkthroughCommentEntity> listChapterComments(UUID chapterId) {
        return commentRepository.findByChapterIdAndWalkthroughFileIdIsNullAndParentIdIsNullOrderByCreatedAtAsc(chapterId);
    }

    @Override
    public List<WalkthroughCommentEntity> listReplies(UUID parentId) {
        return commentRepository.findByParentIdOrderByCreatedAtAsc(parentId);
    }

    @Override
    @Transactional
    public void deleteComment(UUID userId, UUID commentId) {
        WalkthroughCommentEntity comment = commentRepository.findByIdAndUserId(commentId, userId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));
        commentRepository.delete(comment);
    }

    // ── Reading Progress ──

    @Override
    @Transactional
    public ChapterViewEventEntity recordChapterView(UUID userId, UUID walkthroughId, RecordChapterViewRequest request) {
        WalkthroughEntity walkthrough = findWalkthroughById(walkthroughId);

        // Authors reviewing their own walkthrough should not generate progress records
        if (walkthrough.getUserId().equals(userId)) {
            return null;
        }

        // Check first visit BEFORE saving the new event
        boolean isFirstVisit = !chapterViewEventRepository.existsByChapterIdAndUserId(
                request.getChapterId(), userId);

        ChapterViewEventEntity event = ChapterViewEventEntity.builder()
                .chapterId(request.getChapterId())
                .userId(userId)
                .timeSpentSec(request.getTimeSpentSec() != null ? request.getTimeSpentSec() : 0)
                .scrolledToBottom(request.getScrolledToBottom() != null ? request.getScrolledToBottom() : false)
                .viewedAt(Instant.now())
                .build();

        ChapterViewEventEntity saved = chapterViewEventRepository.save(event);

        // Upsert read_progress
        ReadProgressEntity progress = readProgressRepository
                .findByUserIdAndWalkthroughId(userId, walkthroughId)
                .orElse(ReadProgressEntity.builder()
                        .userId(userId)
                        .walkthroughId(walkthroughId)
                        .readChapters(0)
                        .totalChapters(walkthrough.getChapters().size())
                        .timeSpentSec(0)
                        .readAt(Instant.now())
                        .build());

        progress.setLastChapterId(request.getChapterId());
        progress.setTotalChapters(walkthrough.getChapters().size());
        progress.setTimeSpentSec(progress.getTimeSpentSec() +
                (request.getTimeSpentSec() != null ? request.getTimeSpentSec() : 0));
        progress.setReadAt(Instant.now());

        if (isFirstVisit) {
            progress.setReadChapters(progress.getReadChapters() + 1);
        }

        readProgressRepository.save(progress);

        return saved;
    }

    @Override
    public List<ReadProgressEntity> listRecentlyReviewed(UUID userId) {
        return readProgressRepository.findTop10ByUserIdOrderByReadAtDesc(userId)
                .stream()
                .filter(rp -> {
                    WalkthroughEntity wt = walkthroughRepository.findById(rp.getWalkthroughId()).orElse(null);
                    return wt != null && !wt.getUserId().equals(userId);
                })
                .toList();
    }

    @Override
    public ReadProgressEntity getReadProgress(UUID userId, UUID walkthroughId) {
        return readProgressRepository
                .findByUserIdAndWalkthroughId(userId, walkthroughId)
                .orElse(ReadProgressEntity.builder()
                        .userId(userId)
                        .walkthroughId(walkthroughId)
                        .readChapters(0)
                        .totalChapters(0)
                        .timeSpentSec(0)
                        .readAt(Instant.now())
                        .build());
    }

    // ── Private helpers ──

    private WalkthroughEntity findWalkthroughById(UUID id) {
        return walkthroughRepository.findById(id)
                .orElseThrow(() -> new WalkthroughNotFoundException("Walkthrough not found"));
    }

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
