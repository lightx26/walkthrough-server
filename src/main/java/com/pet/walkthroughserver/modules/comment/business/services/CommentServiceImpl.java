package com.pet.walkthroughserver.modules.comment.business.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pet.walkthroughserver.modules.comment.business.events.CommentCreatedEvent;
import com.pet.walkthroughserver.modules.comment.business.events.CommentEventProducer;
import com.pet.walkthroughserver.modules.comment.exceptions.CommentNotFoundException;
import com.pet.walkthroughserver.modules.comment.presentation.dto.CreateCommentRequest;
import com.pet.walkthroughserver.modules.comment.repository.CommentEntity;
import com.pet.walkthroughserver.modules.comment.repository.CommentRepository;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughNotFoundException;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final WalkthroughRepository walkthroughRepository;
    private final CommentEventProducer commentEventProducer;

    @Override
    @Transactional
    public CommentEntity createComment(UUID userId, UUID walkthroughId, CreateCommentRequest request) {
        walkthroughRepository.findById(walkthroughId)
                .orElseThrow(() -> new WalkthroughNotFoundException("Walkthrough not found"));

        CommentEntity comment = CommentEntity.builder()
                .walkthroughId(walkthroughId)
                .userId(userId)
                .content(request.getContent())
                .chapterId(request.getChapterId())
                .walkthroughFileId(request.getWalkthroughFileId())
                .diffPosition(request.getDiffPosition())
                .parentId(request.getParentId())
                .syncStatus("pending")
                .build();

        CommentEntity saved = commentRepository.save(comment);

        // Only publish sync event for line-level comments (those with a file + position)
        if (request.getWalkthroughFileId() != null && request.getDiffPosition() != null) {
            commentEventProducer.publish(CommentCreatedEvent.builder()
                    .commentId(saved.getId())
                    .walkthroughId(walkthroughId)
                    .userId(userId)
                    .content(request.getContent())
                    .build());
        }

        return saved;
    }

    @Override
    public List<CommentEntity> listComments(UUID walkthroughId) {
        return commentRepository.findByWalkthroughIdAndParentIdIsNullOrderByCreatedAtAsc(walkthroughId);
    }

    @Override
    public List<CommentEntity> listFileComments(UUID walkthroughFileId) {
        return commentRepository.findByWalkthroughFileIdAndParentIdIsNullOrderByCreatedAtAsc(walkthroughFileId);
    }

    @Override
    public List<CommentEntity> listChapterComments(UUID chapterId) {
        return commentRepository.findByChapterIdAndWalkthroughFileIdIsNullAndParentIdIsNullOrderByCreatedAtAsc(chapterId);
    }

    @Override
    public List<CommentEntity> listReplies(UUID parentId) {
        return commentRepository.findByParentIdOrderByCreatedAtAsc(parentId);
    }

    @Override
    @Transactional
    public void deleteComment(UUID userId, UUID commentId) {
        CommentEntity comment = commentRepository.findByIdAndUserId(commentId, userId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));
        commentRepository.delete(comment);
    }
}
