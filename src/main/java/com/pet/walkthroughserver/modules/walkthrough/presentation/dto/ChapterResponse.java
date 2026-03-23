package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ChapterResponse {
    private UUID id;
    private String title;
    private String description;
    private Integer sortOrder;
    private List<WalkthroughFileResponse> files;
}
