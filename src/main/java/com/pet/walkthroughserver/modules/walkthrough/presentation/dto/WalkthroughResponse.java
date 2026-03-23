package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class WalkthroughResponse {
    private UUID id;
    private String title;
    private String description;
    private String status;
    private String owner;
    private String repo;
    private Integer prNumber;
    private List<ChapterResponse> chapters;
    private Instant createdAt;
    private Instant updatedAt;
}
