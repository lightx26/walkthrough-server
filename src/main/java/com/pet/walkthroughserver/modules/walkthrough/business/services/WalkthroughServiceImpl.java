package com.pet.walkthroughserver.modules.walkthrough.business.services;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.EntityManager;

import com.pet.walkthroughserver.configs.CacheNames;
import com.pet.walkthroughserver.modules._shared.repository.Repositories;
import com.pet.walkthroughserver.modules._shared.security.OwnershipGuard;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequestFile;
import com.pet.walkthroughserver.modules._shared.messaging.DomainEvent;
import com.pet.walkthroughserver.modules._shared.messaging.DomainEventPublisher;
import com.pet.walkthroughserver.modules.comment.repository.CommentRepository;
import com.pet.walkthroughserver.modules.githubpr.business.services.GitHubPrService;
import com.pet.walkthroughserver.modules.riskzone.repository.RiskScanRepository;
import com.pet.walkthroughserver.modules.riskzone.repository.RiskZoneRepository;
import com.pet.walkthroughserver.modules.walkthrough.business.cache.WalkthroughCacheEvictor;
import com.pet.walkthroughserver.modules.walkthrough.business.mapper.WalkthroughProjectionMapper;
import com.pet.walkthroughserver.modules.walkthrough.business.models.ActivitySummary;
import com.pet.walkthroughserver.modules.walkthrough.business.models.WalkthroughDetail;
import com.pet.walkthroughserver.modules.walkthrough.business.models.WalkthroughSummary;
import com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughCreatedEvent;
import com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughDeletedEvent;
import com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughUpdatedEvent;
import com.pet.walkthroughserver.modules.walkthrough.business.util.PrFileConsistencyChecker;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughAccessDeniedException;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughInvalidForPublishException;
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
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalkthroughServiceImpl implements WalkthroughService {

    private final WalkthroughRepository walkthroughRepository;
    private final GitHubPrService gitHubPrService;
    private final DomainEventPublisher eventPublisher;
    private final CommentRepository commentRepository;
    private final WalkthroughCacheEvictor cacheEvictor;
    private final RiskScanRepository riskScanRepository;
    private final RiskZoneRepository riskZoneRepository;
    private final EntityManager entityManager;
    private final WalkthroughProjectionMapper projectionMapper;

    @Override
    @Transactional
    public WalkthroughEntity create(UUID userId, String username, CreateWalkthroughRequest request) {
        GitHubPullRequest pr = verifyPrOwnership(userId, username, request);

        Map<String, GitHubPullRequestFile> prFilesByName = gitHubPrService
                .getPullRequestFiles(userId, request.getOwner(), request.getRepo(), request.getPrNumber())
                .stream()
                .collect(Collectors.toMap(GitHubPullRequestFile::getFilename, Function.identity(), (a, b) -> a));

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

        buildChapters(walkthrough, request.getChapters(), prFilesByName);

        if (request.getStatus() == WalkthroughStatus.PUBLISHED) {
            validateForPublish(userId, walkthrough.getOwner(), walkthrough.getRepo(),
                    walkthrough.getPrNumber(), request.getChapters());
        }

        WalkthroughEntity saved = walkthroughRepository.save(walkthrough);
        cacheEvictor.onWrite(userId, saved.getId());

        if (saved.getStatus() == WalkthroughStatus.PUBLISHED) {
            archiveOtherPublished(saved);
        }

        publishAfterCommit(new WalkthroughCreatedEvent(saved.getId(), Instant.now()));
        return saved;
    }

    @Override
    @Transactional
    public List<WalkthroughEntity> listByPr(UUID requestingUserId, String owner, String repo, Integer prNumber) {
        List<WalkthroughEntity> all = walkthroughRepository
                .findByOwnerAndRepoAndPrNumberOrderByCreatedAtDesc(owner, repo, prNumber);

        List<WalkthroughEntity> published = all.stream()
                .filter(wt -> wt.getStatus() == WalkthroughStatus.PUBLISHED)
                .toList();

        if (!published.isEmpty()) {
            runConsistencyCheck(requestingUserId, owner, repo, prNumber, published);
        }

        return all.stream()
                .filter(wt -> wt.getStatus() == WalkthroughStatus.PUBLISHED
                        || wt.getStatus() == WalkthroughStatus.OUTDATED
                        || wt.getUserId().equals(requestingUserId))
                .toList();
    }

    @Override
    @Cacheable(value = CacheNames.WALKTHROUGH_RECENT, key = "#userId")
    public List<WalkthroughSummary> listRecent(UUID userId) {
        return projectionMapper.toSummaries(
                walkthroughRepository.findTop10ByUserIdOrderByUpdatedAtDesc(userId));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.WALKTHROUGH_DETAIL, key = "#id")
    public WalkthroughDetail getById(UUID id, UUID requestingUserId) {
        WalkthroughEntity walkthrough = Repositories.orThrow(walkthroughRepository.findByIdWithUser(id),
                () -> new WalkthroughNotFoundException("Walkthrough not found"));
        boolean isOwner = walkthrough.getUserId().equals(requestingUserId);
        if (!isOwner
                && walkthrough.getStatus() != WalkthroughStatus.PUBLISHED
                && walkthrough.getStatus() != WalkthroughStatus.OUTDATED) {
            throw new WalkthroughNotFoundException("Walkthrough not found");
        }
        for (ChapterEntity chapter : walkthrough.getChapters()) {
            for (WalkthroughFileEntity file : chapter.getFiles()) {
                file.getAnnotations().size();
            }
        }
        return projectionMapper.toDetail(walkthrough);
    }

    @Override
    @Transactional
    public WalkthroughEntity syncCheck(UUID requestingUserId, UUID walkthroughId) {
        WalkthroughEntity walkthrough = Repositories.orThrow(walkthroughRepository.findById(walkthroughId),
                () -> new WalkthroughNotFoundException("Walkthrough not found"));

        boolean isOwner = walkthrough.getUserId().equals(requestingUserId);
        if (!isOwner
                && walkthrough.getStatus() != WalkthroughStatus.PUBLISHED
                && walkthrough.getStatus() != WalkthroughStatus.OUTDATED) {
            throw new WalkthroughNotFoundException("Walkthrough not found");
        }

        if (walkthrough.getStatus() == WalkthroughStatus.PUBLISHED) {
            runConsistencyCheck(requestingUserId, walkthrough.getOwner(), walkthrough.getRepo(),
                    walkthrough.getPrNumber(), List.of(walkthrough));
        }

        return walkthrough;
    }

    @Override
    @Transactional
    public WalkthroughEntity update(UUID userId, UUID walkthroughId, UpdateWalkthroughRequest request) {
        WalkthroughEntity walkthrough = findWalkthroughById(walkthroughId);
        verifyOwnership(walkthrough, userId);

        Map<String, GitHubPullRequestFile> prFilesByName = gitHubPrService
                .getPullRequestFiles(userId, walkthrough.getOwner(), walkthrough.getRepo(), walkthrough.getPrNumber())
                .stream()
                .collect(Collectors.toMap(GitHubPullRequestFile::getFilename, Function.identity(), (a, b) -> a));

        if (request.getStatus() == WalkthroughStatus.PUBLISHED) {
            validateForPublish(userId, walkthrough.getOwner(), walkthrough.getRepo(),
                    walkthrough.getPrNumber(), request.getChapters());
        }

        // Capture risk zone → filename mapping before chapters are cleared.
        // Clearing chapters cascade-deletes walkthrough_files, which triggers ON DELETE SET NULL
        // on risk_zones.walkthrough_file_id. We re-link zones to new file IDs after rebuild.
        Map<UUID, String> riskZoneFilenameMap = captureRiskZoneFilenameMap(walkthroughId, walkthrough);

        walkthrough.setTitle(request.getTitle());
        walkthrough.setDescription(request.getDescription());
        walkthrough.setStatus(request.getStatus());
        walkthrough.setOutdatedReason(null);
        walkthrough.getChapters().clear();
        buildChapters(walkthrough, request.getChapters(), prFilesByName);

        WalkthroughEntity saved = walkthroughRepository.save(walkthrough);
        // Flush explicitly so the DELETE of old files (→ SET NULL on risk_zones) and
        // INSERT of new files are committed to the DB before the relink UPDATE runs.
        entityManager.flush();

        relinkRiskZones(saved, riskZoneFilenameMap);

        cacheEvictor.onWrite(userId, saved.getId());
        publishAfterCommit(new WalkthroughUpdatedEvent(saved.getId(), Instant.now()));

        if (saved.getStatus() == WalkthroughStatus.PUBLISHED) {
            archiveOtherPublished(saved);
        }

        return saved;
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID walkthroughId) {
        WalkthroughEntity walkthrough = findWalkthroughById(walkthroughId);
        verifyOwnership(walkthrough, userId);
        walkthroughRepository.delete(walkthrough);
        cacheEvictor.onWrite(userId, walkthroughId);
        publishAfterCommit(new WalkthroughDeletedEvent(walkthroughId, Instant.now()));
    }

    @Override
    @Cacheable(value = CacheNames.WALKTHROUGH_COUNT_REPO, key = "#owner + ':' + #repo + ':' + #requestingUserId")
    public long countByRepo(String owner, String repo, UUID requestingUserId) {
        return walkthroughRepository.countByOwnerAndRepoForUser(owner, repo, requestingUserId);
    }

    @Override
    public long countNonDraftByRepo(String owner, String repo) {
        return walkthroughRepository.countByOwnerAndRepoAndStatusNot(owner, repo, WalkthroughStatus.DRAFT);
    }

    @Override
    @Cacheable(value = CacheNames.WALKTHROUGH_COUNT_PR, key = "#owner + ':' + #repo + ':' + #prNumber + ':' + #requestingUserId")
    public long countByPr(String owner, String repo, int prNumber, UUID requestingUserId) {
        return walkthroughRepository.countByOwnerAndRepoAndPrNumberForUser(owner, repo, prNumber, requestingUserId);
    }

    @Override
    @Cacheable(value = CacheNames.WALKTHROUGH_COUNT_REPOS, key = "#repoFullNames.hashCode() + ':' + #requestingUserId")
    public Map<String, Long> countByRepos(List<String> repoFullNames, UUID requestingUserId) {
        if (repoFullNames.isEmpty()) return Map.of();
        return walkthroughRepository.countByRepoFullNamesForUser(repoFullNames, requestingUserId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Override
    @Cacheable(value = CacheNames.WALKTHROUGH_COUNT_PRS, key = "#owner + ':' + #repo + ':' + #prNumbers.hashCode() + ':' + #requestingUserId")
    public Map<Integer, Long> countByPrs(String owner, String repo, List<Integer> prNumbers, UUID requestingUserId) {
        if (prNumbers.isEmpty()) return Map.of();
        return walkthroughRepository.countByPrNumbersForUser(owner, repo, prNumbers, requestingUserId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (Integer) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Override
    @Cacheable(value = CacheNames.WALKTHROUGH_COMMENT_COUNTS, key = "#walkthroughIds.hashCode()")
    public Map<UUID, Long> getCommentCounts(List<UUID> walkthroughIds) {
        if (walkthroughIds.isEmpty()) return Map.of();
        return commentRepository.countGroupedByWalkthroughIds(walkthroughIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public ActivitySummary getActivitySummary(UUID userId, Instant since) {
        long walkthroughCount = walkthroughRepository.countByUserIdAndCreatedAtGreaterThanEqual(userId, since);
        long commentCount = walkthroughRepository.countCommentsByUserIdSince(userId, since);
        return new ActivitySummary(walkthroughCount, commentCount, since);
    }

    // ── Private helpers ──

    private void runConsistencyCheck(UUID requestingUserId, String owner, String repo, Integer prNumber,
                                     List<WalkthroughEntity> publishedWalkthroughs) {
        Set<String> prFilenames;
        try {
            prFilenames = gitHubPrService.getPullRequestFiles(requestingUserId, owner, repo, prNumber)
                    .stream()
                    .map(GitHubPullRequestFile::getFilename)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("GitHub API call failed during PR consistency check for {}/{} PR#{}: {}",
                    owner, repo, prNumber, e.getMessage());
            return;
        }

        for (WalkthroughEntity walkthrough : publishedWalkthroughs) {
            PrFileConsistencyChecker.Result result = PrFileConsistencyChecker.check(walkthrough, prFilenames);
            if (!result.consistent()) {
                walkthrough.setStatus(WalkthroughStatus.OUTDATED);
                walkthrough.setOutdatedReason(result.outdatedReason());
                walkthroughRepository.save(walkthrough);
                cacheEvictor.onWrite(walkthrough.getUserId(), walkthrough.getId());
                log.info("Walkthrough {} marked OUTDATED: files diverged from PR {}/{} #{}",
                        walkthrough.getId(), owner, repo, prNumber);
            }
        }
    }

    private void validateForPublish(UUID userId, String owner, String repo, Integer prNumber,
                                    List<ChapterRequest> chapters) {
        Set<String> candidateFilenames = chapters.stream()
                .flatMap(ch -> ch.getFiles().stream())
                .map(WalkthroughFileRequest::getFilename)
                .collect(Collectors.toSet());

        Set<String> prFilenames;
        try {
            prFilenames = gitHubPrService.getPullRequestFiles(userId, owner, repo, prNumber)
                    .stream()
                    .map(GitHubPullRequestFile::getFilename)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("GitHub API call failed during publish validation for {}/{} PR#{}: {}",
                    owner, repo, prNumber, e.getMessage());
            throw new WalkthroughInvalidForPublishException(
                    "Cannot verify PR file set. GitHub API is unavailable. Please try again.");
        }

        PrFileConsistencyChecker.Result result = PrFileConsistencyChecker.check(candidateFilenames, prFilenames);
        if (!result.consistent()) {
            throw new WalkthroughInvalidForPublishException(result.outdatedReason());
        }
    }

    private void archiveOtherPublished(WalkthroughEntity published) {
        List<WalkthroughEntity> others = walkthroughRepository
                .findByOwnerAndRepoAndPrNumberAndStatus(
                        published.getOwner(), published.getRepo(),
                        published.getPrNumber(), WalkthroughStatus.PUBLISHED);
        for (WalkthroughEntity other : others) {
            if (!other.getId().equals(published.getId())) {
                other.setStatus(WalkthroughStatus.OUTDATED);
                other.setOutdatedReason("A newer walkthrough was published for this PR.");
                walkthroughRepository.save(other);
            }
        }
    }

    private Map<UUID, String> captureRiskZoneFilenameMap(UUID walkthroughId, WalkthroughEntity walkthrough) {
        // Build file ID → filename from existing chapters (before any modification)
        Map<UUID, String> fileIdToFilename = walkthrough.getChapters().stream()
                .flatMap(ch -> ch.getFiles().stream())
                .collect(Collectors.toMap(WalkthroughFileEntity::getId, WalkthroughFileEntity::getFilename));

        if (fileIdToFilename.isEmpty()) return Map.of();

        return riskScanRepository.findTopByWalkthroughIdOrderByCreatedAtDesc(walkthroughId)
                .map(scan -> scan.getRiskZones().stream()
                        .filter(z -> z.getWalkthroughFileId() != null
                                && fileIdToFilename.containsKey(z.getWalkthroughFileId()))
                        .collect(Collectors.toMap(
                                z -> z.getId(),
                                z -> fileIdToFilename.get(z.getWalkthroughFileId()))))
                .orElse(Map.of());
    }

    private void relinkRiskZones(WalkthroughEntity saved, Map<UUID, String> riskZoneFilenameMap) {
        if (riskZoneFilenameMap.isEmpty()) return;

        Map<String, UUID> newFilenameToFileId = saved.getChapters().stream()
                .flatMap(ch -> ch.getFiles().stream())
                .collect(Collectors.toMap(WalkthroughFileEntity::getFilename, WalkthroughFileEntity::getId, (a, b) -> a));

        riskZoneFilenameMap.forEach((zoneId, filename) -> {
            UUID newFileId = newFilenameToFileId.get(filename);
            if (newFileId != null) {
                // JPQL @Modifying forces a flush first — this is when SET NULL fires and new files are persisted
                riskZoneRepository.relinkToFile(zoneId, newFileId);
            }
            // If newFileId is null, the file was removed from the walkthrough — zone keeps walkthrough_file_id=null
        });
    }

    private WalkthroughEntity findWalkthroughById(UUID id) {
        return Repositories.orThrow(walkthroughRepository.findById(id),
                () -> new WalkthroughNotFoundException("Walkthrough not found"));
    }

    private void verifyOwnership(WalkthroughEntity walkthrough, UUID userId) {
        OwnershipGuard.require(walkthrough.getUserId(), userId,
                () -> new WalkthroughAccessDeniedException("You do not own this walkthrough"));
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

    private void buildChapters(WalkthroughEntity walkthrough, List<ChapterRequest> chapterRequests,
                               Map<String, GitHubPullRequestFile> prFilesByName) {
        for (int i = 0; i < chapterRequests.size(); i++) {
            ChapterRequest cr = chapterRequests.get(i);
            ChapterEntity chapter = ChapterEntity.builder()
                    .walkthrough(walkthrough)
                    .title(cr.getTitle())
                    .description(cr.getDescription())
                    .sortOrder(i)
                    .build();

            buildFiles(chapter, cr.getFiles(), prFilesByName);
            walkthrough.getChapters().add(chapter);
        }
    }

    private void buildFiles(ChapterEntity chapter, List<WalkthroughFileRequest> fileRequests,
                            Map<String, GitHubPullRequestFile> prFilesByName) {
        for (int i = 0; i < fileRequests.size(); i++) {
            WalkthroughFileRequest fr = fileRequests.get(i);
            GitHubPullRequestFile ghFile = prFilesByName.get(fr.getFilename());

            WalkthroughFileEntity file = WalkthroughFileEntity.builder()
                    .chapter(chapter)
                    .filename(fr.getFilename())
                    .fileSha(fr.getFileSha())
                    .fileStatus(fr.getFileStatus())
                    .rawPatch(ghFile != null ? ghFile.getPatch() : fr.getRawPatch())
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

    private void publishAfterCommit(DomainEvent event) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publish(event);
            }
        });
    }
}
