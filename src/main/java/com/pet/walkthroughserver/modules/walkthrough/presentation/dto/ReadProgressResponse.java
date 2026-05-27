package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ReadProgressResponse {
    private UUID id;
    private UUID userId;
    private UUID walkthroughId;
    private UUID lastChapterId;
    private Integer readChapters;
    private Integer totalChapters;
    private Integer timeSpentSec;
    private Instant readAt;

    @Setter
    private List<UUID> readChapterIds;
}
