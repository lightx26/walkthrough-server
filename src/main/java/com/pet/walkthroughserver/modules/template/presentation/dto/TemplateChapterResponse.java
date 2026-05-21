package com.pet.walkthroughserver.modules.template.presentation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class TemplateChapterResponse {
    private UUID id;
    private String title;
    private String description;
    private Integer sortOrder;
}
