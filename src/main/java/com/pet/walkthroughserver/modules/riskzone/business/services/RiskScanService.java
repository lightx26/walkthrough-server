package com.pet.walkthroughserver.modules.riskzone.business.services;

import com.pet.walkthroughserver.modules.riskzone.repository.RiskScanEntity;
import com.pet.walkthroughserver.modules.riskzone.repository.RiskZoneEntity;

import java.util.List;
import java.util.UUID;

public interface RiskScanService {
    RiskScanEntity requestScan(UUID walkthroughId, UUID triggeredBy);
    RiskScanEntity getLatestScan(UUID walkthroughId);
    List<RiskZoneEntity> getRisksForScan(UUID scanId);
}
