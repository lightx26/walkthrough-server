package com.pet.walkthroughserver.modules.profile.presentation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class PinnedWalkthroughResponse {

    private String id;
    private String walkthroughId;
    private String title;
    private String owner;
    private String repo;
    private Integer prNumber;
    private String status;
    private Integer sortOrder;
    private Instant pinnedAt;
}
