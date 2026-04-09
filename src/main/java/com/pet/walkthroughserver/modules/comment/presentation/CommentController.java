package com.pet.walkthroughserver.modules.comment.presentation;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pet.walkthroughserver.interceptors.DataResponse;
import com.pet.walkthroughserver.modules._shared.dto.ListData;
import com.pet.walkthroughserver.modules.comment.business.services.CommentService;
import com.pet.walkthroughserver.modules.comment.presentation.dto.CommentResponse;
import com.pet.walkthroughserver.modules.comment.presentation.dto.CreateCommentRequest;
import com.pet.walkthroughserver.modules.comment.presentation.mapper.CommentPresentationMapper;
import com.pet.walkthroughserver.modules.comment.repository.CommentEntity;
import com.pet.walkthroughserver.security.AuthUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/walkthroughs")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CommentPresentationMapper commentMapper;

    @PostMapping("/{walkthroughId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<CommentResponse>> createComment(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID walkthroughId,
            @Valid @RequestBody CreateCommentRequest request) {
        UUID userId = UUID.fromString(authUser.getUserId());
        CommentEntity entity = commentService.createComment(userId, walkthroughId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(DataResponse.of(commentMapper.toResponse(entity)));
    }

    @GetMapping("/{walkthroughId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<CommentResponse>>> listComments(
            @PathVariable UUID walkthroughId) {
        List<CommentEntity> entities = commentService.listComments(walkthroughId);
        List<CommentResponse> responses = buildThreadedResponses(entities);
        return ResponseEntity.ok(DataResponse.of(ListData.of(responses)));
    }

    @GetMapping("/{walkthroughId}/files/{fileId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<CommentResponse>>> listFileComments(
            @PathVariable UUID walkthroughId,
            @PathVariable UUID fileId) {
        List<CommentEntity> entities = commentService.listFileComments(fileId);
        List<CommentResponse> responses = buildThreadedResponses(entities);
        return ResponseEntity.ok(DataResponse.of(ListData.of(responses)));
    }

    @GetMapping("/{walkthroughId}/chapters/{chapterId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<CommentResponse>>> listChapterComments(
            @PathVariable UUID walkthroughId,
            @PathVariable UUID chapterId) {
        List<CommentEntity> entities = commentService.listChapterComments(chapterId);
        List<CommentResponse> responses = buildThreadedResponses(entities);
        return ResponseEntity.ok(DataResponse.of(ListData.of(responses)));
    }

    @DeleteMapping("/{walkthroughId}/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID walkthroughId,
            @PathVariable UUID commentId) {
        UUID userId = UUID.fromString(authUser.getUserId());
        commentService.deleteComment(userId, commentId);
        return ResponseEntity.noContent().build();
    }

    // ── Private helpers ──

    private List<CommentResponse> buildThreadedResponses(List<CommentEntity> rootComments) {
        return rootComments.stream().map(this::toThreadedResponse).toList();
    }

    private CommentResponse toThreadedResponse(CommentEntity entity) {
        CommentResponse response = commentMapper.toResponse(entity);
        List<CommentEntity> replyEntities = commentService.listReplies(entity.getId());
        response.setReplies(replyEntities.stream().map(this::toThreadedResponse).toList());
        return response;
    }
}
