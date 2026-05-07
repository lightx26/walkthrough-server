package com.pet.walkthroughserver.modules.search.business.services;

import com.pet.walkthroughserver.modules.search.repository.SearchHistoryEntity;

import java.util.List;
import java.util.UUID;

public interface SearchHistoryService {

    void save(UUID userId, String query);

    List<SearchHistoryEntity> getRecent(UUID userId);
}
