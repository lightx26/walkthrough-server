package com.pet.walkthroughserver.modules.search.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pet.walkthroughserver.interceptors.DataResponse;
import com.pet.walkthroughserver.modules.search.business.models.SearchQuery;
import com.pet.walkthroughserver.modules.search.business.models.SearchResult;
import com.pet.walkthroughserver.modules.search.business.services.WalkthroughSearchService;
import com.pet.walkthroughserver.modules.search.presentation.dto.SearchWalkthroughRequest;
import com.pet.walkthroughserver.modules.search.presentation.dto.SearchWalkthroughResponse;
import com.pet.walkthroughserver.modules.search.presentation.mapper.SearchPresentationMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final WalkthroughSearchService searchService;
    private final SearchPresentationMapper mapper;

    @PostMapping("/walkthroughs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<SearchWalkthroughResponse>> search(
            @RequestBody SearchWalkthroughRequest request) {
        SearchQuery query = mapper.toSearchQuery(request);
        SearchResult result = searchService.search(query);
        SearchWalkthroughResponse response = mapper.toResponse(result);
        return ResponseEntity.ok(DataResponse.of(response));
    }
}
