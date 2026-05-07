package com.pet.walkthroughserver.modules.search.presentation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class SearchHistoryResponse {
    private UUID id;
    private String query;
    private Instant createdAt;
}
