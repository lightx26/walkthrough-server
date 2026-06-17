package com.pet.walkthroughserver.modules.riskzone.presentation.dto;

import java.util.List;
import java.util.UUID;

public record RiskScanResponse(
        UUID scanId,
        String status,
        String provider,
        String model,
        int totalFiles,
        int analyzedFiles,
        RiskCountsResponse counts,
        List<RiskFileProgressResponse> fileProgress,
        List<RiskZoneResponse> risks
) {}
