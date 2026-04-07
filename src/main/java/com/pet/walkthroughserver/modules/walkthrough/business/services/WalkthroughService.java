package com.pet.walkthroughserver.modules.walkthrough.business.services;

import java.util.List;
import java.util.UUID;

import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.CreateCommentRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.CreateWalkthroughRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.RecordChapterViewRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.UpdateWalkthroughRequest;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterViewEventEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ReadProgressEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughCommentEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;

public interface WalkthroughService {

    WalkthroughEntity create(UUID userId, String username, CreateWalkthroughRequest request);

    List<WalkthroughEntity> listByPr(String owner, String repo, Integer prNumber, UUID requestingUserId);

    List<WalkthroughEntity> listRecent(UUID userId);

    WalkthroughEntity getById(UUID id, UUID requestingUserId);

    WalkthroughEntity update(UUID userId, UUID walkthroughId, UpdateWalkthroughRequest request);

    void delete(UUID userId, UUID walkthroughId);

    // ── Comments ──

    WalkthroughCommentEntity createComment(UUID userId, UUID walkthroughId, CreateCommentRequest request);

    List<WalkthroughCommentEntity> listComments(UUID walkthroughId);

    List<WalkthroughCommentEntity> listFileComments(UUID walkthroughFileId);

    List<WalkthroughCommentEntity> listChapterComments(UUID chapterId);

    List<WalkthroughCommentEntity> listReplies(UUID parentId);

    void deleteComment(UUID userId, UUID commentId);

    // ── Reading Progress ──

    ChapterViewEventEntity recordChapterView(UUID userId, UUID walkthroughId, RecordChapterViewRequest request);

    ReadProgressEntity getReadProgress(UUID userId, UUID walkthroughId);

    List<ReadProgressEntity> listRecentlyReviewed(UUID userId);
}
