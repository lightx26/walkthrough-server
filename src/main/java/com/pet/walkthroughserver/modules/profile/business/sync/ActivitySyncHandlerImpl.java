package com.pet.walkthroughserver.modules.profile.business.sync;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.modules.profile.repository.ActivityEntryEntity;
import com.pet.walkthroughserver.modules.profile.repository.ActivityEntryRepository;
import com.pet.walkthroughserver.modules.profile.repository.ActivityEventType;
import com.pet.walkthroughserver.modules.profile.repository.ActivityVisibility;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivitySyncHandlerImpl implements ActivitySyncHandler {

    private final ActivityEntryRepository activityEntryRepository;
    private final WalkthroughRepository walkthroughRepository;

    @Override
    public void handle(ActivitySyncCommand command) {
        log.info("Activity sync: processing event {} for walkthrough {}",
                command.eventType(), command.walkthroughId());

        Optional<WalkthroughEntity> walkthroughOpt = walkthroughRepository.findById(command.walkthroughId());
        if (walkthroughOpt.isEmpty() && !"WALKTHROUGH_DELETED".equals(command.eventType())) {
            log.warn("Walkthrough {} not found, skipping activity entry", command.walkthroughId());
            return;
        }

        switch (command.eventType()) {
            case "WALKTHROUGH_CREATED" -> handleWalkthroughCreated(walkthroughOpt.get(), command);
            case "WALKTHROUGH_UPDATED" -> handleWalkthroughUpdated(walkthroughOpt.get(), command);
            case "WALKTHROUGH_DELETED" -> log.debug("Walkthrough deleted, no activity entry needed");
            default -> log.warn("Unknown event type: {}", command.eventType());
        }
    }

    private void handleWalkthroughCreated(WalkthroughEntity walkthrough, ActivitySyncCommand command) {
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
                .occurredAt(command.occurredAt())
                .walkthroughId(walkthrough.getId())
                .metadata(metadata)
                .visibility(visibility)
                .build();

        activityEntryRepository.save(entry);
    }

    private void handleWalkthroughUpdated(WalkthroughEntity walkthrough, ActivitySyncCommand command) {
        if (walkthrough.getStatus() == WalkthroughStatus.PUBLISHED) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", walkthrough.getTitle());
            metadata.put("repoFull", walkthrough.getOwner() + "/" + walkthrough.getRepo());
            metadata.put("prNumber", walkthrough.getPrNumber());

            ActivityEntryEntity entry = ActivityEntryEntity.builder()
                    .userId(walkthrough.getUserId())
                    .eventType(ActivityEventType.WALKTHROUGH_PUBLISHED)
                    .occurredAt(command.occurredAt())
                    .walkthroughId(walkthrough.getId())
                    .metadata(metadata)
                    .visibility(ActivityVisibility.PUBLIC)
                    .build();

            activityEntryRepository.save(entry);
        }
    }
}
