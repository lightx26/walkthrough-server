package com.pet.walkthroughserver.modules.riskzone.business.sync;

import com.pet.walkthroughserver.modules._shared.messaging.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record RiskScanRequestedEvent(
        UUID scanId,
        UUID walkthroughId,
        Instant occurredAt
) implements DomainEvent {

    public static RiskScanRequestedEvent of(UUID scanId, UUID walkthroughId) {
        return new RiskScanRequestedEvent(scanId, walkthroughId, Instant.now());
    }

    @Override public UUID aggregateId()  { return scanId; }
    @Override public String eventType()  { return "RISK_SCAN_REQUESTED"; }
}
