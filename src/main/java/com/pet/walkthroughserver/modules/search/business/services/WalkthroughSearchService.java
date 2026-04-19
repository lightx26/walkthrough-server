package com.pet.walkthroughserver.modules.search.business.services;

import com.pet.walkthroughserver.modules.search.business.models.SearchQuery;
import com.pet.walkthroughserver.modules.search.business.models.SearchResult;

public interface WalkthroughSearchService {
    SearchResult search(SearchQuery query);
}
