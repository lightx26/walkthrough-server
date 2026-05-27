package com.pet.walkthroughserver.modules.walkthrough.business.services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pet.walkthroughserver.configs.CacheNames;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughNotFoundException;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.RecordChapterViewRequest;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterReadMarkEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterReadMarkRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterViewEventEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterViewEventRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.ReadProgressEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ReadProgressRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReadProgressServiceImpl implements ReadProgressService {

    private final WalkthroughRepository walkthroughRepository;
    private final ChapterViewEventRepository chapterViewEventRepository;
    private final ChapterReadMarkRepository chapterReadMarkRepository;
    private final ReadProgressRepository readProgressRepository;

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.WALKTHROUGH_PROGRESS, key = "#userId + ':' + #walkthroughId")
    public ChapterViewEventEntity recordChapterView(UUID userId, UUID walkthroughId, RecordChapterViewRequest request) {
        WalkthroughEntity walkthrough = walkthroughRepository.findById(walkthroughId)
                .orElseThrow(() -> new WalkthroughNotFoundException("Walkthrough not found"));

        // Authors don't generate progress for their own walkthrough
        if (walkthrough.getUserId().equals(userId)) {
            return null;
        }

        int timeSpent = request.getTimeSpentSec() != null ? request.getTimeSpentSec() : 0;

        ChapterViewEventEntity event = ChapterViewEventEntity.builder()
                .chapterId(request.getChapterId())
                .userId(userId)
                .timeSpentSec(timeSpent)
                .viewedAt(Instant.now())
                .build();
        ChapterViewEventEntity saved = chapterViewEventRepository.save(event);

        ReadProgressEntity progress = loadOrInitProgress(userId, walkthrough);
        progress.setLastChapterId(request.getChapterId());
        progress.setTotalChapters(walkthrough.getChapters().size());
        progress.setTimeSpentSec(progress.getTimeSpentSec() + timeSpent);
        progress.setReadAt(Instant.now());
        readProgressRepository.save(progress);

        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.WALKTHROUGH_PROGRESS, key = "#userId + ':' + #walkthroughId")
    public void markChapterRead(UUID userId, UUID walkthroughId, UUID chapterId) {
        WalkthroughEntity walkthrough = walkthroughRepository.findById(walkthroughId)
                .orElseThrow(() -> new WalkthroughNotFoundException("Walkthrough not found"));

        if (walkthrough.getUserId().equals(userId)) {
            return;
        }

        if (!chapterReadMarkRepository.existsByUserIdAndChapterId(userId, chapterId)) {
            chapterReadMarkRepository.save(ChapterReadMarkEntity.builder()
                    .userId(userId)
                    .walkthroughId(walkthroughId)
                    .chapterId(chapterId)
                    .markedAt(Instant.now())
                    .build());
        }

        ReadProgressEntity progress = loadOrInitProgress(userId, walkthrough);
        progress.setTotalChapters(walkthrough.getChapters().size());
        progress.setReadChapters((int) chapterReadMarkRepository.countByUserIdAndWalkthroughId(userId, walkthroughId));
        progress.setReadAt(Instant.now());
        readProgressRepository.save(progress);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.WALKTHROUGH_PROGRESS, key = "#userId + ':' + #walkthroughId")
    public void unmarkChapterRead(UUID userId, UUID walkthroughId, UUID chapterId) {
        chapterReadMarkRepository.deleteByUserIdAndChapterId(userId, chapterId);

        readProgressRepository.findByUserIdAndWalkthroughIdForUpdate(userId, walkthroughId)
                .ifPresent(progress -> {
                    progress.setReadChapters(
                            (int) chapterReadMarkRepository.countByUserIdAndWalkthroughId(userId, walkthroughId));
                    readProgressRepository.save(progress);
                });
    }

    @Override
    public List<ReadProgressEntity> listRecentlyReviewed(UUID userId) {
        return readProgressRepository.findRecentlyReviewed(userId);
    }

    @Override
    public List<UUID> getReadChapterIds(UUID userId, UUID walkthroughId) {
        return chapterReadMarkRepository.findMarkedChapterIds(userId, walkthroughId);
    }

    @Override
    @Cacheable(value = CacheNames.WALKTHROUGH_PROGRESS, key = "#userId + ':' + #walkthroughId")
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

    private ReadProgressEntity loadOrInitProgress(UUID userId, WalkthroughEntity walkthrough) {
        return readProgressRepository
                .findByUserIdAndWalkthroughIdForUpdate(userId, walkthrough.getId())
                .orElse(ReadProgressEntity.builder()
                        .userId(userId)
                        .walkthroughId(walkthrough.getId())
                        .readChapters(0)
                        .totalChapters(walkthrough.getChapters().size())
                        .timeSpentSec(0)
                        .readAt(Instant.now())
                        .build());
    }
}
