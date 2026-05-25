package com.pet.walkthroughserver.modules.template.presentation.dto;

import com.pet.walkthroughserver.modules.template.repository.TemplatePrType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class TemplateSummaryResponse {
    private UUID id;
    private String name;
    private String description;
    private TemplatePrType prType;
    private Boolean isBuiltin;
    private Long duplicateCount;
    private Integer chapterCount;
    private Instant updatedAt;
}
