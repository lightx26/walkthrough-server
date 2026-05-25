package com.pet.walkthroughserver.modules.comment.business.services;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.pet.walkthroughserver.modules.comment.presentation.dto.CreateCommentRequest;
import com.pet.walkthroughserver.modules.comment.repository.CommentEntity;

public interface CommentService {

    CommentEntity createComment(UUID userId, UUID walkthroughId, CreateCommentRequest request);

    List<CommentEntity> listComments(UUID walkthroughId);

    List<CommentEntity> listFileComments(UUID walkthroughFileId);

    Map<UUID, List<CommentEntity>> listBatchFileComments(List<UUID> walkthroughFileIds);

    List<CommentEntity> listChapterComments(UUID chapterId);

    List<CommentEntity> listReplies(UUID parentId);

    void deleteComment(UUID userId, UUID commentId);
}
