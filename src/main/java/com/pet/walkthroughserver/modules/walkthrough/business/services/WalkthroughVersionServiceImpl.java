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
import com.pet.walkthroughserver.modules.githubpr.business.services.GitHubPrService;
import com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughEventPublisher;
import com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughUpdatedEvent;
import com.pet.walkthroughserver.modules.walkthrough.business.util.DiffLineMapper;
import com.pet.walkthroughserver.modules.walkthrough.business.util.WalkthroughSnapshotSerializer;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughAccessDeniedException;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughNotFoundException;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.StalenessResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.VersionDiffResponse;
import com.pet.walkthroughserver.modules.walkthrough.repository.AnnotationEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.AnnotationStatus;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughSnapshotEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughSnapshotRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalkthroughVersionServiceImpl implements WalkthroughVersionService {

    private final WalkthroughRepository walkthroughRepository;
    private final WalkthroughSnapshotRepository snapshotRepository;
    private final GitHubPrService gitHubPrService;
    private final WalkthroughEventPublisher walkthroughEventPublisher;

    // ── 5.1 Detect stale walkthrough ──

    @Override
    public StalenessResponse checkStaleness(UUID userId, UUID walkthroughId) {
        WalkthroughEntity walkthrough = findWalkthrough(walkthroughId);
        verifyOwnership(walkthrough, userId);

        GitHubPullRequest pr = gitHubPrService.getPullRequest(
                userId, walkthrough.getOwner(), walkthrough.getRepo(), walkthrough.getPrNumber());

        String latestSha = pr.getHead() != null ? pr.getHead().getSha() : null;
        boolean stale = latestSha != null && !latestSha.equals(walkthrough.getCommitSha());

        return StalenessResponse.builder()
                .stale(stale)
                .currentCommitSha(walkthrough.getCommitSha())
                .latestCommitSha(latestSha)
                .currentVersion(walkthrough.getVersion())
                .build();
    }

    // ── 5.2 Create new walkthrough version ──

    @Override
    @Transactional
    public WalkthroughEntity createNewVersion(UUID userId, UUID walkthroughId) {
        WalkthroughEntity walkthrough = findWalkthrough(walkthroughId);
        verifyOwnership(walkthrough, userId);

        // 1. Snapshot current state before modification
        captureSnapshot(walkthrough);

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
    public VersionDiffResponse getVersionDiff(UUID userId, UUID walkthroughId, int fromVersion, int toVersion) {
        WalkthroughEntity walkthrough = findWalkthrough(walkthroughId);

        // Get snapshots
        Map<String, Object> fromContent = getSnapshotContent(walkthroughId, fromVersion, walkthrough);
        Map<String, Object> toContent = getSnapshotContent(walkthroughId, toVersion, walkthrough);

        String fromCommitSha = getStringField(fromContent, "commitSha");
        String toCommitSha = getStringField(toContent, "commitSha");

        // Compare chapters
        List<VersionDiffResponse.ChapterDiff> chapterDiffs = compareChapters(fromContent, toContent);

        // Collect outdated annotations from the target version
        List<VersionDiffResponse.AnnotationDiff> outdatedAnnotations = collectOutdatedAnnotations(toContent);

        return VersionDiffResponse.builder()
                .walkthroughId(walkthroughId)
                .fromVersion(fromVersion)
                .toVersion(toVersion)
                .fromCommitSha(fromCommitSha)
                .toCommitSha(toCommitSha)
                .chapters(chapterDiffs)
                .outdatedAnnotations(outdatedAnnotations)
                .build();
    }

    // ── Private helpers ──

    private void captureSnapshot(WalkthroughEntity walkthrough) {
        // Don't duplicate if snapshot already exists for this version
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
        log.info("Captured snapshot for walkthrough {} version {}", walkthrough.getId(), walkthrough.getVersion());
    }

    private void validateAnnotations(WalkthroughFileEntity file, String oldRawPatch) {
        String newRawPatch = file.getRawPatch();

        for (AnnotationEntity annotation : file.getAnnotations()) {
            if (annotation.getStatus() == AnnotationStatus.OUTDATED) {
                continue; // already marked
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> getSnapshotContent(UUID walkthroughId, int version, WalkthroughEntity walkthrough) {
        // If requesting current version and no snapshot yet, serialize live data
        if (version == walkthrough.getVersion()) {
            return snapshotRepository.findByWalkthroughIdAndVersion(walkthroughId, version)
                    .map(WalkthroughSnapshotEntity::getWalkthroughContent)
                    .orElseGet(() -> WalkthroughSnapshotSerializer.serialize(walkthrough));
        }

        return snapshotRepository.findByWalkthroughIdAndVersion(walkthroughId, version)
                .map(WalkthroughSnapshotEntity::getWalkthroughContent)
                .orElseThrow(() -> new WalkthroughNotFoundException(
                        "Snapshot not found for walkthrough " + walkthroughId + " version " + version));
    }

    @SuppressWarnings("unchecked")
    private List<VersionDiffResponse.ChapterDiff> compareChapters(
            Map<String, Object> fromContent, Map<String, Object> toContent) {

        List<Map<String, Object>> fromChapters = getListField(fromContent, "chapters");
        List<Map<String, Object>> toChapters = getListField(toContent, "chapters");

        // Index chapters by title for matching
        Map<String, Map<String, Object>> fromByTitle = new LinkedHashMap<>();
        for (Map<String, Object> ch : fromChapters) {
            fromByTitle.put(getStringField(ch, "title"), ch);
        }
        Map<String, Map<String, Object>> toByTitle = new LinkedHashMap<>();
        for (Map<String, Object> ch : toChapters) {
            toByTitle.put(getStringField(ch, "title"), ch);
        }

        List<VersionDiffResponse.ChapterDiff> diffs = new ArrayList<>();

        // Process all "to" chapters
        for (Map<String, Object> toCh : toChapters) {
            String title = getStringField(toCh, "title");
            Map<String, Object> fromCh = fromByTitle.remove(title);

            if (fromCh == null) {
                // New chapter
                diffs.add(VersionDiffResponse.ChapterDiff.builder()
                        .changeType("ADDED")
                        .title(title)
                        .toTitle(title)
                        .files(extractFileDiffs(null, getListField(toCh, "files")))
                        .build());
            } else {
                // Existing chapter — compare files
                List<VersionDiffResponse.FileDiff> fileDiffs =
                        extractFileDiffs(getListField(fromCh, "files"), getListField(toCh, "files"));

                boolean hasChanges = fileDiffs.stream()
                        .anyMatch(f -> !"UNCHANGED".equals(f.getChangeType()));

                diffs.add(VersionDiffResponse.ChapterDiff.builder()
                        .changeType(hasChanges ? "MODIFIED" : "UNCHANGED")
                        .title(title)
                        .fromTitle(title)
                        .toTitle(title)
                        .files(fileDiffs)
                        .build());
            }
        }

        // Remaining fromByTitle entries are removed chapters
        for (Map.Entry<String, Map<String, Object>> entry : fromByTitle.entrySet()) {
            diffs.add(VersionDiffResponse.ChapterDiff.builder()
                    .changeType("REMOVED")
                    .title(entry.getKey())
                    .fromTitle(entry.getKey())
                    .files(extractFileDiffs(getListField(entry.getValue(), "files"), null))
                    .build());
        }

        return diffs;
    }

    @SuppressWarnings("unchecked")
    private List<VersionDiffResponse.FileDiff> extractFileDiffs(
            List<Map<String, Object>> fromFiles, List<Map<String, Object>> toFiles) {

        List<VersionDiffResponse.FileDiff> diffs = new ArrayList<>();

        if (fromFiles == null) fromFiles = List.of();
        if (toFiles == null) toFiles = List.of();

        Map<String, Map<String, Object>> fromByName = new LinkedHashMap<>();
        for (Map<String, Object> f : fromFiles) {
            fromByName.put(getStringField(f, "filename"), f);
        }

        for (Map<String, Object> toFile : toFiles) {
            String filename = getStringField(toFile, "filename");
            Map<String, Object> fromFile = fromByName.remove(filename);

            if (fromFile == null) {
                diffs.add(VersionDiffResponse.FileDiff.builder()
                        .changeType("ADDED")
                        .filename(filename)
                        .toFilename(filename)
                        .build());
            } else {
                String fromSha = getStringField(fromFile, "fileSha");
                String toSha = getStringField(toFile, "fileSha");
                boolean modified = fromSha != null && !fromSha.equals(toSha);

                diffs.add(VersionDiffResponse.FileDiff.builder()
                        .changeType(modified ? "MODIFIED" : "UNCHANGED")
                        .filename(filename)
                        .fromFilename(filename)
                        .toFilename(filename)
                        .build());
            }
        }

        for (String removedFilename : fromByName.keySet()) {
            diffs.add(VersionDiffResponse.FileDiff.builder()
                    .changeType("REMOVED")
                    .filename(removedFilename)
                    .fromFilename(removedFilename)
                    .build());
        }

        return diffs;
    }

    @SuppressWarnings("unchecked")
    private List<VersionDiffResponse.AnnotationDiff> collectOutdatedAnnotations(Map<String, Object> content) {
        List<VersionDiffResponse.AnnotationDiff> outdated = new ArrayList<>();

        List<Map<String, Object>> chapters = getListField(content, "chapters");
        for (Map<String, Object> chapter : chapters) {
            List<Map<String, Object>> files = getListField(chapter, "files");
            for (Map<String, Object> file : files) {
                String filename = getStringField(file, "filename");
                List<Map<String, Object>> annotations = getListField(file, "annotations");

                for (Map<String, Object> annotation : annotations) {
                    String status = getStringField(annotation, "status");
                    if ("OUTDATED".equals(status)) {
                        outdated.add(VersionDiffResponse.AnnotationDiff.builder()
                                .annotationId(UUID.fromString(getStringField(annotation, "id")))
                                .filename(filename)
                                .startLine(getIntField(annotation, "startLine"))
                                .endLine(getIntField(annotation, "endLine"))
                                .lineSide(getStringField(annotation, "lineSide"))
                                .content(getStringField(annotation, "content"))
                                .reason("DIFF_CHANGED")
                                .build());
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
                walkthroughEventPublisher.publish(event);
            }
        });
    }

    // ── JSON Map helpers ──

    private static String getStringField(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private static int getIntField(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val != null) return Integer.parseInt(val.toString());
        return 0;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getListField(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> list) return (List<Map<String, Object>>) list;
        return List.of();
    }
}
