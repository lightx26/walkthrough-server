package com.pet.walkthroughserver.modules.walkthrough.business.services;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.pet.walkthroughserver.configs.CacheNames;

import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequestFile;
import com.pet.walkthroughserver.modules.comment.repository.CommentRepository;
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
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.WALKTHROUGH_RECENT, key = "#userId"),
            @CacheEvict(value = CacheNames.WALKTHROUGH_COUNT_REPO, allEntries = true),
            @CacheEvict(value = CacheNames.WALKTHROUGH_COUNT_REPOS, allEntries = true),
            @CacheEvict(value = CacheNames.WALKTHROUGH_COUNT_PR, allEntries = true),
            @CacheEvict(value = CacheNames.WALKTHROUGH_COUNT_PRS, allEntries = true),
            @CacheEvict(value = CacheNames.PROFILE_STATS, allEntries = true)
    })
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
    @Cacheable(value = CacheNames.WALKTHROUGH_RECENT, key = "#userId")
    public List<WalkthroughEntity> listRecent(UUID userId) {
        return walkthroughRepository.findTop10ByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Override
    @Cacheable(value = CacheNames.WALKTHROUGH_DETAIL, key = "#id")
    public WalkthroughEntity getById(UUID id, UUID requestingUserId) {
        WalkthroughEntity walkthrough = walkthroughRepository.findByIdWithUser(id)
                .orElseThrow(() -> new WalkthroughNotFoundException("Walkthrough not found"));
        boolean isOwner = walkthrough.getUserId().equals(requestingUserId);
        if (!isOwner && walkthrough.getStatus() != WalkthroughStatus.PUBLISHED) {
            throw new WalkthroughNotFoundException("Walkthrough not found");
        }
        return walkthrough;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.WALKTHROUGH_DETAIL, key = "#walkthroughId"),
            @CacheEvict(value = CacheNames.WALKTHROUGH_RECENT, key = "#userId"),
            @CacheEvict(value = CacheNames.WALKTHROUGH_COUNT_REPO, allEntries = true),
            @CacheEvict(value = CacheNames.WALKTHROUGH_COUNT_REPOS, allEntries = true),
            @CacheEvict(value = CacheNames.WALKTHROUGH_COUNT_PR, allEntries = true),
            @CacheEvict(value = CacheNames.WALKTHROUGH_COUNT_PRS, allEntries = true),
            @CacheEvict(value = CacheNames.WALKTHROUGH_COMMENT_COUNTS, allEntries = true),
            @CacheEvict(value = CacheNames.PROFILE_STATS, allEntries = true)
    })
    public WalkthroughEntity update(UUID userId, UUID walkthroughId, UpdateWalkthroughRequest request) {
        WalkthroughEntity walkthrough = findWalkthroughById(walkthroughId);
        verifyOwnership(walkthrough, userId);

        Map<String, GitHubPullRequestFile> prFilesByName = gitHubPrService
                .getPullRequestFiles(userId, walkthrough.getOwner(), walkthrough.getRepo(), walkthrough.getPrNumber())
                .stream()
                .collect(Collectors.toMap(GitHubPullRequestFile::getFilename, Function.identity(), (a, b) -> a));

        walkthrough.setTitle(request.getTitle());
        walkthrough.setDescription(request.getDescription());
        walkthrough.setStatus(request.getStatus());
        walkthrough.getChapters().clear();
        buildChapters(walkthrough, request.getChapters(), prFilesByName);

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
    @Caching(evict = {
            @CacheEvict(value = CacheNames.WALKTHROUGH_DETAIL, key = "#walkthroughId"),
            @CacheEvict(value = CacheNames.WALKTHROUGH_RECENT, key = "#userId"),
            @CacheEvict(value = CacheNames.WALKTHROUGH_COUNT_REPO, allEntries = true),
            @CacheEvict(value = CacheNames.WALKTHROUGH_COUNT_REPOS, allEntries = true),
            @CacheEvict(value = CacheNames.WALKTHROUGH_COUNT_PR, allEntries = true),
            @CacheEvict(value = CacheNames.WALKTHROUGH_COUNT_PRS, allEntries = true),
            @CacheEvict(value = CacheNames.WALKTHROUGH_COMMENT_COUNTS, allEntries = true),
            @CacheEvict(value = CacheNames.PROFILE_STATS, allEntries = true)
    })
    public void delete(UUID userId, UUID walkthroughId) {
        WalkthroughEntity walkthrough = findWalkthroughById(walkthroughId);
        verifyOwnership(walkthrough, userId);
        walkthroughRepository.delete(walkthrough);
        publishAfterCommit(new WalkthroughDeletedEvent(walkthroughId, Instant.now()));
    }

    @Override
    @Cacheable(value = CacheNames.WALKTHROUGH_COUNT_REPO, key = "#owner + ':' + #repo + ':' + #requestingUserId")
    public long countByRepo(String owner, String repo, UUID requestingUserId) {
        return walkthroughRepository.countByOwnerAndRepoForUser(owner, repo, requestingUserId);
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

    // ── Private helpers ──

    private void archiveOtherPublished(WalkthroughEntity published) {
        List<WalkthroughEntity> others = walkthroughRepository
                .findByOwnerAndRepoAndPrNumberAndStatus(
                        published.getOwner(), published.getRepo(),
                        published.getPrNumber(), WalkthroughStatus.PUBLISHED);
        for (WalkthroughEntity other : others) {
            if (!other.getId().equals(published.getId())) {
                other.setStatus(WalkthroughStatus.OUTDATED);
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

    private void publishAfterCommit(com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughEvent event) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                walkthroughEventPublisher.publish(event);
            }
        });
    }
}
