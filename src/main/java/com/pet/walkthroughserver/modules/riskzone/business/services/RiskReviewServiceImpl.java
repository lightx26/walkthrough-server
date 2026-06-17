package com.pet.walkthroughserver.modules.riskzone.business.services;

import com.pet.walkthroughserver.modules.riskzone.exceptions.RiskScanNotFoundException;
import com.pet.walkthroughserver.modules.riskzone.repository.ReviewStatus;
import com.pet.walkthroughserver.modules.riskzone.repository.RiskZoneEntity;
import com.pet.walkthroughserver.modules.riskzone.repository.RiskZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RiskReviewServiceImpl implements RiskReviewService {

    private final RiskZoneRepository riskZoneRepository;

    @Override
    @Transactional
    public RiskZoneEntity markReviewed(UUID riskId, boolean reviewed) {
        RiskZoneEntity zone = riskZoneRepository.findById(riskId)
                .orElseThrow(() -> new RiskScanNotFoundException("Risk zone not found: " + riskId));
        zone.setReviewStatus(reviewed ? ReviewStatus.REVIEWED : ReviewStatus.OPEN);
        return riskZoneRepository.save(zone);
    }
}
