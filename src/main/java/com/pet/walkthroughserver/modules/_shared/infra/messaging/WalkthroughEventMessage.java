package com.pet.walkthroughserver.modules._shared.infra.messaging;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalkthroughEventMessage {

    private UUID eventId;
    private String eventType;
    private UUID walkthroughId;
    private String occurredAt;
    private int schemaVersion;
}
