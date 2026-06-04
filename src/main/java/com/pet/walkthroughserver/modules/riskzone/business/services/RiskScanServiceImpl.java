package com.pet.walkthroughserver.modules.riskzone.business.services;

import com.pet.walkthroughserver.modules._shared.messaging.DomainEventPublisher;
import com.pet.walkthroughserver.modules.riskzone.business.sync.RiskScanRequestedEvent;
import com.pet.walkthroughserver.modules.riskzone.exceptions.RiskScanInProgressException;
import com.pet.walkthroughserver.modules.riskzone.exceptions.RiskScanNotFoundException;
import com.pet.walkthroughserver.modules.riskzone.repository.*;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughNotFoundException;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RiskScanServiceImpl implements RiskScanService {

    private final RiskScanRepository riskScanRepository;
    private final RiskZoneRepository riskZoneRepository;
    private final WalkthroughRepository walkthroughRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    @Transactional
    public RiskScanEntity requestScan(UUID walkthroughId, UUID triggeredBy) {
        if (!walkthroughRepository.existsById(walkthroughId)) {
            throw new WalkthroughNotFoundException("Walkthrough not found: " + walkthroughId);
        }
        if (riskScanRepository.existsByWalkthroughIdAndStatus(walkthroughId, RiskScanStatus.ANALYZING)) {
            throw new RiskScanInProgressException("A risk scan is already in progress for this walkthrough");
        }

        RiskScanEntity scan = RiskScanEntity.builder()
                .walkthroughId(walkthroughId)
                .triggeredBy(triggeredBy)
                .status(RiskScanStatus.PENDING)
                .build();
        riskScanRepository.save(scan);

        eventPublisher.publish(RiskScanRequestedEvent.of(scan.getId(), walkthroughId));
        return scan;
    }

    @Override
    @Transactional(readOnly = true)
    public RiskScanEntity getLatestScan(UUID walkthroughId) {
        if (!walkthroughRepository.existsById(walkthroughId)) {
            throw new WalkthroughNotFoundException("Walkthrough not found: " + walkthroughId);
        }
        return riskScanRepository.findTopByWalkthroughIdOrderByCreatedAtDesc(walkthroughId)
                .orElseThrow(() -> new RiskScanNotFoundException("No scan found for walkthrough: " + walkthroughId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiskZoneEntity> getRisksForScan(UUID scanId) {
        return riskZoneRepository.findByRiskScanId(scanId);
    }
}
