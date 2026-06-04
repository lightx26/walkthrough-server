package com.pet.walkthroughserver.modules._shared.infra.messaging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RiskScanEventMessage {

    private UUID scanId;
    private UUID walkthroughId;
    private String occurredAt;
}
