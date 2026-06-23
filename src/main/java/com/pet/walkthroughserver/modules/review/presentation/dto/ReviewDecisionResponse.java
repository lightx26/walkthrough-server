package com.pet.walkthroughserver.modules.review.presentation.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ReviewDecisionResponse {
    private UUID id;
    private UUID walkthroughId;
    private UUID userId;
    private String username;
    private String avatarUrl;
    private String decision;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;
}
