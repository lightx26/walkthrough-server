package com.pet.walkthroughserver.modules.starredrepo.presentation.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StarredRepoResponse {

    private UUID id;
    private String repoFullName;
    private String repoName;
    private String language;
    private Instant createdAt;
}
