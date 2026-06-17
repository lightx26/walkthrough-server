package com.pet.walkthroughserver.modules.riskzone.business.services;

import com.pet.walkthroughserver.modules.riskzone.repository.RiskZoneEntity;

import java.util.UUID;

public interface RiskReviewService {
    RiskZoneEntity markReviewed(UUID riskId, boolean reviewed);
}
