package com.pet.walkthroughserver.modules.riskzone.business.models;

import com.pet.walkthroughserver.modules.riskzone.repository.RiskCategory;
import com.pet.walkthroughserver.modules.riskzone.repository.RiskLevel;

public record DetectedRisk(
        RiskLevel riskLevel,
        RiskCategory category,
        String title,
        String description,
        String suggestion,
        Integer startPosition,
        Integer endPosition,
        String lineSide
) {}
