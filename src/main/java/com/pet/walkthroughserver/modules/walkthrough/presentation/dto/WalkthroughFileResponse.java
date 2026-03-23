package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class WalkthroughFileResponse {
    private UUID id;
    private String filename;
    private String fileSha;
    private String fileStatus;
    private Integer sortOrder;
    private List<AnnotationResponse> annotations;
}
