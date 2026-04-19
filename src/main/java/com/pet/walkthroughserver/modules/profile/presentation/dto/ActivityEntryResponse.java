package com.pet.walkthroughserver.modules.profile.presentation.dto;

import com.pet.walkthroughserver.modules.profile.repository.ActivityEventType;
import com.pet.walkthroughserver.modules.profile.repository.ActivityVisibility;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class ActivityEntryResponse {

    private String id;
    private ActivityEventType eventType;
    private Instant occurredAt;
    private String walkthroughId;
    private String chapterId;
    private String commentId;
    private ActivityVisibility visibility;
    private Map<String, Object> metadata;
}
