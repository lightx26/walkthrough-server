package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CommentResponse {
    private UUID id;
    private UUID walkthroughId;
    private UUID userId;
    private String username;
    private String avatarUrl;
    private String content;
    private String syncStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
