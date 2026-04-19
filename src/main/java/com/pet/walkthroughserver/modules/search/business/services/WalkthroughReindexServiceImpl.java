package com.pet.walkthroughserver.modules.search.business.services;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.pet.walkthroughserver.modules.search.business.models.WalkthroughDocument;
import com.pet.walkthroughserver.modules.search.infra.WalkthroughDocumentMapper;
import com.pet.walkthroughserver.modules.search.infra.WalkthroughEsRepository;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalkthroughReindexServiceImpl implements WalkthroughReindexService {

    private final WalkthroughRepository walkthroughRepository;
    private final WalkthroughEsRepository esRepository;
    private final WalkthroughDocumentMapper documentMapper;
    private final UserService userService;

    @Override
    public int reindexAll() {
        log.info("Starting full reindex of all walkthroughs");
        AtomicInteger count = new AtomicInteger(0);
        int pageSize = 100;
        int pageNumber = 0;

        Page<WalkthroughEntity> page;
        do {
            page = walkthroughRepository.findAll(
                    PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").ascending())
            );

            for (WalkthroughEntity entity : page.getContent()) {
                try {
                    UserEntity user = userService.findById(entity.getUserId());
                    WalkthroughDocument document = documentMapper.toDocument(entity, user.getUsername());
                    esRepository.index(document);
                    count.incrementAndGet();
                } catch (Exception e) {
                    log.error("Failed to index walkthrough {} during reindex", entity.getId(), e);
                }
            }

            pageNumber++;
        } while (page.hasNext());

        log.info("Completed full reindex: {} walkthroughs indexed", count.get());
        return count.get();
    }
}
