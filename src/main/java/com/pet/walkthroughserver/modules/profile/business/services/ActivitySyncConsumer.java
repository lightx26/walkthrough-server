package com.pet.walkthroughserver.modules.profile.business.services;

import com.pet.walkthroughserver.configs.RabbitMQConfig;
import com.pet.walkthroughserver.modules._shared.infra.messaging.WalkthroughEventMessage;
import com.pet.walkthroughserver.modules.profile.repository.ActivityEntryEntity;
import com.pet.walkthroughserver.modules.profile.repository.ActivityEntryRepository;
import com.pet.walkthroughserver.modules.profile.repository.ActivityEventType;
import com.pet.walkthroughserver.modules.profile.repository.ActivityVisibility;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivitySyncConsumer {

    private final ActivityEntryRepository activityEntryRepository;
    private final WalkthroughRepository walkthroughRepository;

    @RabbitListener(queues = RabbitMQConfig.ACTIVITY_SYNC_QUEUE)
    public void handleWalkthroughEvent(WalkthroughEventMessage message) {
        log.info("Activity sync: processing event {} for walkthrough {}",
                message.getEventType(), message.getWalkthroughId());

        Optional<WalkthroughEntity> walkthroughOpt = walkthroughRepository.findById(message.getWalkthroughId());
        if (walkthroughOpt.isEmpty() && !"walkthrough.deleted".equals(message.getEventType())) {
            log.warn("Walkthrough {} not found, skipping activity entry", message.getWalkthroughId());
            return;
        }

        switch (message.getEventType()) {
            case "walkthrough.created" -> handleWalkthroughCreated(walkthroughOpt.get(), message);
            case "walkthrough.updated" -> handleWalkthroughUpdated(walkthroughOpt.get(), message);
            case "walkthrough.deleted" -> log.debug("Walkthrough deleted, no activity entry needed");
            default -> log.warn("Unknown event type: {}", message.getEventType());
        }
    }

    private void handleWalkthroughCreated(WalkthroughEntity walkthrough, WalkthroughEventMessage message) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", walkthrough.getTitle());
        metadata.put("status", walkthrough.getStatus().name());
        metadata.put("repoFull", walkthrough.getOwner() + "/" + walkthrough.getRepo());
        metadata.put("prNumber", walkthrough.getPrNumber());

        ActivityVisibility visibility = walkthrough.getStatus() == WalkthroughStatus.PUBLISHED
                ? ActivityVisibility.PUBLIC : ActivityVisibility.PRIVATE;

        ActivityEntryEntity entry = ActivityEntryEntity.builder()
                .userId(walkthrough.getUserId())
                .eventType(ActivityEventType.WALKTHROUGH_CREATED)
                .occurredAt(Instant.parse(message.getOccurredAt()))
                .walkthroughId(walkthrough.getId())
                .metadata(metadata)
                .visibility(visibility)
                .build();

        activityEntryRepository.save(entry);
    }

    private void handleWalkthroughUpdated(WalkthroughEntity walkthrough, WalkthroughEventMessage message) {
        // Create a PUBLISHED event if the walkthrough is now published
        if (walkthrough.getStatus() == WalkthroughStatus.PUBLISHED) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", walkthrough.getTitle());
            metadata.put("repoFull", walkthrough.getOwner() + "/" + walkthrough.getRepo());
            metadata.put("prNumber", walkthrough.getPrNumber());

            ActivityEntryEntity entry = ActivityEntryEntity.builder()
                    .userId(walkthrough.getUserId())
                    .eventType(ActivityEventType.WALKTHROUGH_PUBLISHED)
                    .occurredAt(Instant.parse(message.getOccurredAt()))
                    .walkthroughId(walkthrough.getId())
                    .metadata(metadata)
                    .visibility(ActivityVisibility.PUBLIC)
                    .build();

            activityEntryRepository.save(entry);
        }
    }
}
