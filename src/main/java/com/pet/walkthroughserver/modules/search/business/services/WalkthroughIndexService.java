package com.pet.walkthroughserver.modules.search.business.services;

import java.util.UUID;

import com.pet.walkthroughserver.modules.search.business.models.WalkthroughDocument;

public interface WalkthroughIndexService {
    void index(WalkthroughDocument document);
    void delete(UUID walkthroughId);
}
