package com.pet.walkthroughserver.modules.search.business.services;

import org.springframework.stereotype.Service;

import com.pet.walkthroughserver.modules.search.business.models.SearchQuery;
import com.pet.walkthroughserver.modules.search.business.models.SearchResult;
import com.pet.walkthroughserver.modules.search.infra.WalkthroughEsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalkthroughSearchServiceImpl implements WalkthroughSearchService {

    private final WalkthroughEsRepository esRepository;

    @Override
    public SearchResult search(SearchQuery query) {
        return esRepository.search(query);
    }
}
