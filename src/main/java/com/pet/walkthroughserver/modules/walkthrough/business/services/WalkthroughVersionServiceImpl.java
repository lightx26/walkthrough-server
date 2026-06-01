package com.pet.walkthroughserver.modules.walkthrough.business.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequestFile;
import com.pet.walkthroughserver.modules._shared.repository.Repositories;
import com.pet.walkthroughserver.modules._shared.security.OwnershipGuard;
import com.pet.walkthroughserver.modules._shared.messaging.DomainEventPublisher;
import com.pet.walkthroughserver.modules.githubpr.business.services.GitHubPrService;
import com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughUpdatedEvent;
import com.pet.walkthroughserver.modules.walkthrough.business.models.SnapshotContent;
import com.pet.walkthroughserver.modules.walkthrough.business.models.StalenessResult;
import com.pet.walkthroughserver.modules.walkthrough.business.models.VersionDiff;
import com.pet.walkthroughserver.modules.walkthrough.business.util.DiffLineMapper;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughAccessDeniedException;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughNotFoundException;
import com.pet.walkthroughserver.modules.walkthrough.repository.AnnotationEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.AnnotationStatus;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalkthroughVersionServiceImpl implements WalkthroughVersionService {

    private final WalkthroughRepository walkthroughRepository;
    private final WalkthroughSnapshotService snapshotService;
    private final GitHubPrService gitHubPrService;
    private final DomainEventPublisher eventPublisher;

    // ── 5.1 Detect stale walkthrough ──

    @Override
    public StalenessResult checkStaleness(UUID userId, UUID walkthroughId) {
        WalkthroughEntity walkthrough = findWalkthrough(walkthroughId);
        verifyOwnership(walkthrough, userId);

        GitHubPullRequest pr = gitHubPrService.getPullRequest(
                userId, walkthrough.getOwner(), walkthrough.getRepo(), walkthrough.getPrNumber());

        String latestSha = pr.getHead() != null ? pr.getHead().getSha() : null;
        boolean stale = latestSha != null && !latestSha.equals(walkthrough.getCommitSha());

        return new StalenessResult(stale, walkthrough.getCommitSha(), latestSha, walkthrough.getVersion());
    }

    // ── 5.2 Create new walkthrough version ──

    @Override
    @Transactional
    public WalkthroughEntity createNewVersion(UUID userId, UUID walkthroughId) {
        WalkthroughEntity walkthrough = findWalkthrough(walkthroughId);
        verifyOwnership(walkthrough, userId);

        // 1. Snapshot current state before modification
        snapshotService.captureSnapshot(walkthrough);

        // 2. Fetch fresh PR data
        GitHubPullRequest pr = gitHubPrService.getPullRequest(
                userId, walkthrough.getOwner(), walkthrough.getRepo(), walkthrough.getPrNumber());
        String newCommitSha = pr.getHead() != null ? pr.getHead().getSha() : null;

        List<GitHubPullRequestFile> freshFiles = gitHubPrService.getPullRequestFiles(
                userId, walkthrough.getOwner(), walkthrough.getRepo(), walkthrough.getPrNumber());

        // 3. Build a map of new files by filename for efficient lookup
        Map<String, GitHubPullRequestFile> freshFileMap = freshFiles.stream()
                .collect(Collectors.toMap(
                        GitHubPullRequestFile::getFilename,
                        Function.identity(),
                        (a, b) -> b // in case of duplicates, take the last
                ));

        // 4. Increment version, update commit_sha, set status to DRAFT
        int newVersion = walkthrough.getVersion() + 1;
        walkthrough.setVersion(newVersion);
        walkthrough.setCommitSha(newCommitSha);
        walkthrough.setStatus(WalkthroughStatus.DRAFT);

        // 5. Update each file in each chapter with fresh diff data,
        //    and validate annotations
        for (ChapterEntity chapter : walkthrough.getChapters()) {
            List<WalkthroughFileEntity> filesToRemove = new ArrayList<>();

            for (WalkthroughFileEntity file : chapter.getFiles()) {
                GitHubPullRequestFile freshFile = freshFileMap.get(file.getFilename());

                if (freshFile == null) {
                    // File no longer in the PR — mark all annotations as OUTDATED
                    for (AnnotationEntity annotation : file.getAnnotations()) {
                        annotation.setStatus(AnnotationStatus.OUTDATED);
                    }
                    filesToRemove.add(file);
                    continue;
                }

                // Update file with fresh data
                String oldRawPatch = file.getRawPatch();
                file.setFileSha(freshFile.getSha());
                file.setFileStatus(freshFile.getStatus());
                file.setRawPatch(freshFile.getPatch());

                // 5.3 Validate annotations against the new diff
                validateAnnotations(file, oldRawPatch);

                // Remove from the map so we know which files are truly new
                freshFileMap.remove(file.getFilename());
            }

            // Remove files that are no longer in the PR
            chapter.getFiles().removeAll(filesToRemove);
        }

        // 6. Save and publish event
        WalkthroughEntity saved = walkthroughRepository.save(walkthrough);

        publishAfterCommit(new WalkthroughUpdatedEvent(saved.getId(), Instant.now()));

        log.info("Created new version {} for walkthrough {}", newVersion, walkthroughId);
        return saved;
    }

    // ── 5.4 Walkthrough Diff View ──

    @Override
    public VersionDiff getVersionDiff(UUID userId, UUID walkthroughId, int fromVersion, int toVersion) {
        WalkthroughEntity walkthrough = findWalkthrough(walkthroughId);

        SnapshotContent fromContent = snapshotService.getSnapshotContent(walkthroughId, fromVersion, walkthrough);
        SnapshotContent toContent = snapshotService.getSnapshotContent(walkthroughId, toVersion, walkthrough);

        List<VersionDiff.ChapterDiff> chapterDiffs = compareChapters(fromContent, toContent);
        List<VersionDiff.AnnotationDiff> outdatedAnnotations = collectOutdatedAnnotations(toContent);

        return new VersionDiff(
                walkthroughId, fromVersion, toVersion,
                fromContent.commitSha(), toContent.commitSha(),
                chapterDiffs, outdatedAnnotations);
    }

    // ── Private helpers ──

    private void validateAnnotations(WalkthroughFileEntity file, String oldRawPatch) {
        String newRawPatch = file.getRawPatch();

        for (AnnotationEntity annotation : file.getAnnotations()) {
            if (annotation.getStatus() == AnnotationStatus.OUTDATED) {
                continue;
            }

            boolean valid = newRawPatch != null && DiffLineMapper.isRangeValid(
                    newRawPatch,
                    annotation.getLineSide(),
                    annotation.getStartLine(),
                    annotation.getEndLine()
            );

            if (!valid) {
                annotation.setStatus(AnnotationStatus.OUTDATED);
                log.debug("Annotation {} marked OUTDATED: lines {}-{} no longer valid in new diff",
                        annotation.getId(), annotation.getStartLine(), annotation.getEndLine());
            }
        }
    }

    private List<VersionDiff.ChapterDiff> compareChapters(SnapshotContent fromContent, SnapshotContent toContent) {
        List<SnapshotContent.SnapshotChapter> fromChapters = fromContent.chapters() != null ? fromContent.chapters() : List.of();
        List<SnapshotContent.SnapshotChapter> toChapters = toContent.chapters() != null ? toContent.chapters() : List.of();

        Map<String, SnapshotContent.SnapshotChapter> fromByTitle = new LinkedHashMap<>();
        for (SnapshotContent.SnapshotChapter ch : fromChapters) {
            fromByTitle.put(ch.title(), ch);
        }

        List<VersionDiff.ChapterDiff> diffs = new ArrayList<>();

        for (SnapshotContent.SnapshotChapter toCh : toChapters) {
            String title = toCh.title();
            SnapshotContent.SnapshotChapter fromCh = fromByTitle.remove(title);

            if (fromCh == null) {
                diffs.add(new VersionDiff.ChapterDiff("ADDED", title, null, title,
                        extractFileDiffs(null, toCh.files())));
            } else {
                List<VersionDiff.FileDiff> fileDiffs = extractFileDiffs(fromCh.files(), toCh.files());
                boolean hasChanges = fileDiffs.stream()
                        .anyMatch(f -> !"UNCHANGED".equals(f.changeType()));
                diffs.add(new VersionDiff.ChapterDiff(
                        hasChanges ? "MODIFIED" : "UNCHANGED", title, title, title, fileDiffs));
            }
        }

        for (Map.Entry<String, SnapshotContent.SnapshotChapter> entry : fromByTitle.entrySet()) {
            diffs.add(new VersionDiff.ChapterDiff("REMOVED", entry.getKey(), entry.getKey(), null,
                    extractFileDiffs(entry.getValue().files(), null)));
        }

        return diffs;
    }

    private List<VersionDiff.FileDiff> extractFileDiffs(
            List<SnapshotContent.SnapshotFile> fromFiles, List<SnapshotContent.SnapshotFile> toFiles) {

        if (fromFiles == null) fromFiles = List.of();
        if (toFiles == null) toFiles = List.of();

        Map<String, SnapshotContent.SnapshotFile> fromByName = new LinkedHashMap<>();
        for (SnapshotContent.SnapshotFile f : fromFiles) {
            fromByName.put(f.filename(), f);
        }

        List<VersionDiff.FileDiff> diffs = new ArrayList<>();

        for (SnapshotContent.SnapshotFile toFile : toFiles) {
            String filename = toFile.filename();
            SnapshotContent.SnapshotFile fromFile = fromByName.remove(filename);

            if (fromFile == null) {
                diffs.add(new VersionDiff.FileDiff("ADDED", filename, null, filename));
            } else {
                boolean modified = fromFile.fileSha() != null && !fromFile.fileSha().equals(toFile.fileSha());
                diffs.add(new VersionDiff.FileDiff(
                        modified ? "MODIFIED" : "UNCHANGED", filename, filename, filename));
            }
        }

        for (String removedFilename : fromByName.keySet()) {
            diffs.add(new VersionDiff.FileDiff("REMOVED", removedFilename, removedFilename, null));
        }

        return diffs;
    }

    private List<VersionDiff.AnnotationDiff> collectOutdatedAnnotations(SnapshotContent content) {
        List<VersionDiff.AnnotationDiff> outdated = new ArrayList<>();

        List<SnapshotContent.SnapshotChapter> chapters = content.chapters() != null ? content.chapters() : List.of();
        for (SnapshotContent.SnapshotChapter chapter : chapters) {
            List<SnapshotContent.SnapshotFile> files = chapter.files() != null ? chapter.files() : List.of();
            for (SnapshotContent.SnapshotFile file : files) {
                List<SnapshotContent.SnapshotAnnotation> annotations = file.annotations() != null ? file.annotations() : List.of();
                for (SnapshotContent.SnapshotAnnotation annotation : annotations) {
                    if ("OUTDATED".equals(annotation.status())) {
                        outdated.add(new VersionDiff.AnnotationDiff(
                                UUID.fromString(annotation.id()),
                                file.filename(),
                                annotation.startLine(),
                                annotation.endLine(),
                                annotation.lineSide(),
                                annotation.content(),
                                "DIFF_CHANGED"));
                    }
                }
            }
        }

        return outdated;
    }

    private WalkthroughEntity findWalkthrough(UUID id) {
        return Repositories.orThrow(walkthroughRepository.findById(id),
                () -> new WalkthroughNotFoundException("Walkthrough not found"));
    }

    private void verifyOwnership(WalkthroughEntity walkthrough, UUID userId) {
        OwnershipGuard.require(walkthrough.getUserId(), userId,
                () -> new WalkthroughAccessDeniedException("You do not own this walkthrough"));
    }

    private void publishAfterCommit(WalkthroughUpdatedEvent event) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publish(event);
            }
        });
    }
}
