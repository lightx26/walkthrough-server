package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActivitySummaryResponse {
    private long walkthroughCount;
    private long commentCount;
    private Instant since;
}
