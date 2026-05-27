package com.pet.walkthroughserver.modules.comment.business.services;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.pet.walkthroughserver.configs.CacheNames;
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
    @CacheEvict(value = CacheNames.WALKTHROUGH_COMMENT_COUNTS, allEntries = true)
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

        CommentCreatedEvent event = CommentCreatedEvent.builder()
                .commentId(saved.getId())
                .walkthroughId(walkthroughId)
                .userId(userId)
                .content(request.getContent())
                .walkthroughFileId(request.getWalkthroughFileId())
                .diffPosition(request.getDiffPosition())
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                commentEventProducer.publish(event);
            }
        });

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
    public Map<UUID, List<CommentEntity>> listBatchFileComments(List<UUID> walkthroughFileIds) {
        if (walkthroughFileIds.isEmpty()) return Map.of();
        return commentRepository.findByWalkthroughFileIdInAndParentIdIsNullOrderByCreatedAtAsc(walkthroughFileIds)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(CommentEntity::getWalkthroughFileId));
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
    @CacheEvict(value = CacheNames.WALKTHROUGH_COMMENT_COUNTS, allEntries = true)
    public void deleteComment(UUID userId, UUID commentId) {
        CommentEntity comment = commentRepository.findByIdAndUserId(commentId, userId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));
        commentRepository.delete(comment);
    }

}
