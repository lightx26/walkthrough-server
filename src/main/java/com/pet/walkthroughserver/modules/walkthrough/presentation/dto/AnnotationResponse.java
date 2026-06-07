package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AnnotationResponse {
    private UUID id;
    private Integer startLine;
    private Integer endLine;
    private String lineSide;
    private String content;
    private Integer sortOrder;
}
