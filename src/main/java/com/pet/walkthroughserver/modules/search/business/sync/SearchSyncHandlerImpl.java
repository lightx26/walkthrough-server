package com.pet.walkthroughserver.modules.search.business.sync;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pet.walkthroughserver.modules.search.business.models.WalkthroughDocument;
import com.pet.walkthroughserver.modules.search.business.services.WalkthroughIndexService;
import com.pet.walkthroughserver.modules.search.infra.WalkthroughDocumentMapper;
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
public class SearchSyncHandlerImpl implements SearchSyncHandler {

    private final WalkthroughIndexService indexService;
    private final WalkthroughRepository walkthroughRepository;
    private final UserService userService;
    private final WalkthroughDocumentMapper documentMapper;

    @Override
    @Transactional(readOnly = true)
    public void handle(SearchSyncCommand command) {
        log.info("Search sync: processing {} for walkthrough {}",
                command.eventType(), command.walkthroughId());

        try {
            switch (command.eventType()) {
                case "WALKTHROUGH_CREATED", "WALKTHROUGH_UPDATED" -> indexWalkthrough(command.walkthroughId());
                case "WALKTHROUGH_DELETED" -> indexService.delete(command.walkthroughId());
                default -> log.warn("Unknown event type: {}", command.eventType());
            }
        } catch (WalkthroughNotFoundException e) {
            log.info("Walkthrough {} not found during indexing, skipping (already deleted)",
                    command.walkthroughId());
        } catch (Exception e) {
            log.error("Failed to process event {} for walkthrough {}",
                    command.eventType(), command.walkthroughId(), e);
            throw e;
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
