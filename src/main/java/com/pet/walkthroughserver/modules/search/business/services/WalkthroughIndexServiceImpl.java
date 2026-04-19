package com.pet.walkthroughserver.modules.search.business.services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pet.walkthroughserver.modules.search.business.models.WalkthroughDocument;
import com.pet.walkthroughserver.modules.search.infra.WalkthroughEsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalkthroughIndexServiceImpl implements WalkthroughIndexService {

    private final WalkthroughEsRepository esRepository;

    @Override
    public void index(WalkthroughDocument document) {
        esRepository.index(document);
        log.info("Indexed walkthrough {}", document.getId());
    }

    @Override
    public void delete(UUID walkthroughId) {
        esRepository.delete(walkthroughId);
        log.info("Deleted walkthrough {} from index", walkthroughId);
    }
}
