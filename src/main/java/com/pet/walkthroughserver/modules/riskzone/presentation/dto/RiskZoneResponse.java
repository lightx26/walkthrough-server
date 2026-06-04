package com.pet.walkthroughserver.modules.riskzone.presentation.dto;

import java.util.UUID;

public record RiskZoneResponse(
        UUID id,
        String level,
        String category,
        String categoryLabel,
        String title,
        String description,
        String suggestion,
        String filename,
        UUID walkthroughFileId,
        Integer startPosition,
        Integer endPosition,
        String lineSide,
        String reviewStatus
) {}
