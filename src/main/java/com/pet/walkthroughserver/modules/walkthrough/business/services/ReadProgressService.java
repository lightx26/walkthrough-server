package com.pet.walkthroughserver.modules.walkthrough.business.services;

import java.util.List;
import java.util.UUID;

import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.RecordChapterViewRequest;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterViewEventEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ReadProgressEntity;

public interface ReadProgressService {

    ChapterViewEventEntity recordChapterView(UUID userId, UUID walkthroughId, RecordChapterViewRequest request);

    ReadProgressEntity getReadProgress(UUID userId, UUID walkthroughId);

    List<ReadProgressEntity> listRecentlyReviewed(UUID userId);
}
