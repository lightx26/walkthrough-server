package com.pet.walkthroughserver.modules.walkthrough.business.services;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules.githubpr.business.services.GitHubPrService;
import com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughCreatedEvent;
import com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughDeletedEvent;
import com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughEventPublisher;
import com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughUpdatedEvent;
import com.pet.walkthroughserver.modules.walkthrough.business.util.WalkthroughSnapshotSerializer;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughAccessDeniedException;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughNotFoundException;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.AnnotationRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.ChapterRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.CreateWalkthroughRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.UpdateWalkthroughRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.WalkthroughFileRequest;
import com.pet.walkthroughserver.modules.walkthrough.repository.AnnotationEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughSnapshotEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughSnapshotRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalkthroughServiceImpl implements WalkthroughService {

    private final WalkthroughRepository walkthroughRepository;
    private final WalkthroughSnapshotRepository snapshotRepository;
    private final GitHubPrService gitHubPrService;
    private final WalkthroughEventPublisher walkthroughEventPublisher;

    @Override
    @Transactional
    public WalkthroughEntity create(UUID userId, String username, CreateWalkthroughRequest request) {
        GitHubPullRequest pr = verifyPrOwnership(userId, username, request);

        WalkthroughEntity walkthrough = WalkthroughEntity.builder()
                .userId(userId)
                .owner(request.getOwner())
                .repo(request.getRepo())
                .prNumber(request.getPrNumber())
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus())
                .commitSha(pr.getHead() != null ? pr.getHead().getSha() : null)
                .build();

        buildChapters(walkthrough, request.getChapters());
        WalkthroughEntity saved = walkthroughRepository.save(walkthrough);
        publishAfterCommit(new WalkthroughCreatedEvent(saved.getId(), Instant.now()));
        return saved;
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
        boolean isOwner = walkthrough.getUserId().equals(requestingUserId);
        if (!isOwner && walkthrough.getStatus() != WalkthroughStatus.PUBLISHED) {
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

        WalkthroughEntity saved = walkthroughRepository.save(walkthrough);
        publishAfterCommit(new WalkthroughUpdatedEvent(saved.getId(), Instant.now()));

        // When publishing, archive any other published walkthroughs in the same PR
        if (saved.getStatus() == WalkthroughStatus.PUBLISHED) {
            archiveOtherPublished(saved);
            captureSnapshot(saved);
        }

        return saved;
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID walkthroughId) {
        WalkthroughEntity walkthrough = findWalkthroughById(walkthroughId);
        verifyOwnership(walkthrough, userId);
        walkthroughRepository.delete(walkthrough);
        publishAfterCommit(new WalkthroughDeletedEvent(walkthroughId, Instant.now()));
    }

    // ── Private helpers ──

    private void archiveOtherPublished(WalkthroughEntity published) {
        List<WalkthroughEntity> others = walkthroughRepository
                .findByOwnerAndRepoAndPrNumberAndStatus(
                        published.getOwner(), published.getRepo(),
                        published.getPrNumber(), WalkthroughStatus.PUBLISHED);
        for (WalkthroughEntity other : others) {
            if (!other.getId().equals(published.getId())) {
                other.setStatus(WalkthroughStatus.DEPRECATED);
                walkthroughRepository.save(other);
            }
        }
    }

    private void captureSnapshot(WalkthroughEntity walkthrough) {
        if (snapshotRepository.findByWalkthroughIdAndVersion(
                walkthrough.getId(), walkthrough.getVersion()).isPresent()) {
            return;
        }

        Map<String, Object> content = WalkthroughSnapshotSerializer.serialize(walkthrough);

        WalkthroughSnapshotEntity snapshot = WalkthroughSnapshotEntity.builder()
                .walkthroughId(walkthrough.getId())
                .version(walkthrough.getVersion())
                .commitSha(walkthrough.getCommitSha())
                .walkthroughContent(content)
                .build();

        snapshotRepository.save(snapshot);
    }

    private WalkthroughEntity findWalkthroughById(UUID id) {
        return walkthroughRepository.findById(id)
                .orElseThrow(() -> new WalkthroughNotFoundException("Walkthrough not found"));
    }

    private void verifyOwnership(WalkthroughEntity walkthrough, UUID userId) {
        if (!walkthrough.getUserId().equals(userId)) {
            throw new WalkthroughAccessDeniedException("You do not own this walkthrough");
        }
    }

    private GitHubPullRequest verifyPrOwnership(UUID userId, String username, CreateWalkthroughRequest request) {
        GitHubPullRequest pr = gitHubPrService.getPullRequest(
                userId, request.getOwner(), request.getRepo(), request.getPrNumber());
        String prAuthorLogin = pr.getUser().getLogin();
        if (!username.equalsIgnoreCase(prAuthorLogin)) {
            throw new WalkthroughAccessDeniedException(
                    "Only the PR owner can create a walkthrough for this pull request");
        }
        return pr;
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
                    .rawPatch(fr.getRawPatch())
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

    private void publishAfterCommit(com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughEvent event) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                walkthroughEventPublisher.publish(event);
            }
        });
    }
}
