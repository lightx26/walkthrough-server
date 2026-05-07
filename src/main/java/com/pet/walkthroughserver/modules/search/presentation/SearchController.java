package com.pet.walkthroughserver.modules.search.presentation;

import com.pet.walkthroughserver.interceptors.DataResponse;
import com.pet.walkthroughserver.modules._shared.dto.ListData;
import com.pet.walkthroughserver.modules.search.business.models.SearchQuery;
import com.pet.walkthroughserver.modules.search.business.models.SearchResult;
import com.pet.walkthroughserver.modules.search.business.services.SearchHistoryService;
import com.pet.walkthroughserver.modules.search.business.services.WalkthroughSearchService;
import com.pet.walkthroughserver.modules.search.presentation.dto.SaveSearchHistoryRequest;
import com.pet.walkthroughserver.modules.search.presentation.dto.SearchHistoryResponse;
import com.pet.walkthroughserver.modules.search.presentation.dto.SearchWalkthroughRequest;
import com.pet.walkthroughserver.modules.search.presentation.dto.SearchWalkthroughResponse;
import com.pet.walkthroughserver.modules.search.presentation.mapper.SearchPresentationMapper;
import com.pet.walkthroughserver.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final WalkthroughSearchService searchService;
    private final SearchHistoryService searchHistoryService;
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

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<SearchHistoryResponse>>> getHistory(
            @AuthenticationPrincipal AuthUser authUser) {
        UUID userId = UUID.fromString(authUser.getUserId());
        List<SearchHistoryResponse> history = searchHistoryService.getRecent(userId).stream()
                .map(h -> SearchHistoryResponse.builder()
                        .id(h.getId())
                        .query(h.getQuery())
                        .createdAt(h.getCreatedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(DataResponse.of(ListData.of(history)));
    }

    @PostMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> saveHistory(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody SaveSearchHistoryRequest request) {
        UUID userId = UUID.fromString(authUser.getUserId());
        searchHistoryService.save(userId, request.getQuery());
        return ResponseEntity.noContent().build();
    }
}
