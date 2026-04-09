package com.pet.walkthroughserver.modules.comment.presentation.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class CommentResponse {
    private UUID id;
    private UUID walkthroughId;
    private UUID userId;
    private String username;
    private String avatarUrl;
    private String content;
    private UUID chapterId;
    private UUID walkthroughFileId;
    private Integer diffPosition;
    private UUID parentId;
    private String syncStatus;
    private List<CommentResponse> replies;
    private Instant createdAt;
    private Instant updatedAt;
}
