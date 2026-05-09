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
    private final ReadProgressRepository readProgressRepository;

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.WALKTHROUGH_PROGRESS, key = "#userId + ':' + #walkthroughId")
    public ChapterViewEventEntity recordChapterView(UUID userId, UUID walkthroughId, RecordChapterViewRequest request) {
        WalkthroughEntity walkthrough = walkthroughRepository.findById(walkthroughId)
                .orElseThrow(() -> new WalkthroughNotFoundException("Walkthrough not found"));

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
                .findByUserIdAndWalkthroughIdForUpdate(userId, walkthroughId)
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
        return readProgressRepository.findRecentlyReviewed(userId);
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
}
