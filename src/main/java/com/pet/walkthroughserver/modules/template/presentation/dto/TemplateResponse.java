package com.pet.walkthroughserver.modules.template.presentation.dto;

import com.pet.walkthroughserver.modules.template.repository.TemplatePrType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class TemplateResponse {
    private UUID id;
    private String name;
    private String description;
    private TemplatePrType prType;
    private Boolean isBuiltin;
    private List<TemplateChapterResponse> chapters;
    private Instant createdAt;
    private Instant updatedAt;
}
