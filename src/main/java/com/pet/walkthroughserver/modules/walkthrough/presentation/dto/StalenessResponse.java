package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StalenessResponse {
    private boolean stale;
    private String currentCommitSha;
    private String latestCommitSha;
    private Integer currentVersion;
}
