package com.pet.walkthroughserver.modules.riskzone.business.services;

import com.pet.walkthroughserver.modules.riskzone.business.models.DetectedRisk;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;

import java.util.List;

public interface RiskDetectionService {
    List<DetectedRisk> detect(WalkthroughFileEntity file);
}
