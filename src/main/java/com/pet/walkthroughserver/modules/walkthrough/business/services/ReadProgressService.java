package com.pet.walkthroughserver.modules.walkthrough.business.services;

import java.util.List;
import java.util.UUID;

import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.RecordChapterViewRequest;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterViewEventEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ReadProgressEntity;

public interface ReadProgressService {

    ChapterViewEventEntity recordChapterView(UUID userId, UUID walkthroughId, RecordChapterViewRequest request);

    void markChapterRead(UUID userId, UUID walkthroughId, UUID chapterId);

    void unmarkChapterRead(UUID userId, UUID walkthroughId, UUID chapterId);

    ReadProgressEntity getReadProgress(UUID userId, UUID walkthroughId);

    List<UUID> getReadChapterIds(UUID userId, UUID walkthroughId);

    List<ReadProgressEntity> listRecentlyReviewed(UUID userId);
}
