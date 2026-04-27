package com.pet.walkthroughserver.modules.search.infra;

import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pet.walkthroughserver.configs.RabbitMQConfig;
import com.pet.walkthroughserver.modules._shared.infra.messaging.WalkthroughEventMessage;
import com.pet.walkthroughserver.modules.search.business.models.WalkthroughDocument;
import com.pet.walkthroughserver.modules.search.business.services.WalkthroughIndexService;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughNotFoundException;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalkthroughSearchSyncConsumer {

    private final WalkthroughIndexService indexService;
    private final WalkthroughRepository walkthroughRepository;
    private final UserService userService;
    private final WalkthroughDocumentMapper documentMapper;

    @RabbitListener(queues = RabbitMQConfig.WALKTHROUGH_SEARCH_SYNC_QUEUE)
    @Transactional(readOnly = true)
    public void handleWalkthroughEvent(WalkthroughEventMessage message) {
        log.info("Received {} for walkthrough {}", message.getEventType(), message.getWalkthroughId());

        try {
            switch (message.getEventType()) {
                case "WALKTHROUGH_CREATED", "WALKTHROUGH_UPDATED" -> indexWalkthrough(message.getWalkthroughId());
                case "WALKTHROUGH_DELETED" -> indexService.delete(message.getWalkthroughId());
                default -> log.warn("Unknown event type: {}", message.getEventType());
            }
        } catch (WalkthroughNotFoundException e) {
            // Walkthrough already deleted, ACK the message (idempotent)
            log.info("Walkthrough {} not found during indexing, skipping (already deleted)", message.getWalkthroughId());
        } catch (Exception e) {
            log.error("Failed to process event {} for walkthrough {}", message.getEventType(), message.getWalkthroughId(), e);
            throw e; // Will be retried / sent to DLQ
        }
    }

    private void indexWalkthrough(UUID walkthroughId) {
        WalkthroughEntity walkthrough = walkthroughRepository.findById(walkthroughId)
                .orElseThrow(() -> new WalkthroughNotFoundException("Walkthrough not found"));
        UserEntity user = userService.findById(walkthrough.getUserId());
        WalkthroughDocument document = documentMapper.toDocument(walkthrough, user.getUsername());
        indexService.index(document);
    }
}
