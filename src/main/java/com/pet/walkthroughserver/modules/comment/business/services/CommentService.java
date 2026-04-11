package com.pet.walkthroughserver.modules.comment.business.services;

import com.pet.walkthroughserver.modules.comment.presentation.dto.CreateCommentRequest;
import com.pet.walkthroughserver.modules.comment.repository.CommentEntity;

import java.util.List;
import java.util.UUID;

public interface CommentService {

    CommentEntity createComment(UUID userId, UUID walkthroughId, CreateCommentRequest request);

    Long createPrComment(UUID userId, String owner, String repo, int prNumber, String body);

    Long createPrReviewComment(UUID userId, String owner, String repo, int prNumber,
                                String body, String commitId, String path, int position);

    List<CommentEntity> listComments(UUID walkthroughId);

    List<CommentEntity> listFileComments(UUID walkthroughFileId);

    List<CommentEntity> listChapterComments(UUID chapterId);

    List<CommentEntity> listReplies(UUID parentId);

    void deleteComment(UUID userId, UUID commentId);
}
