package com.pet.walkthroughserver.modules.search.presentation.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.modules.search.business.models.SearchQuery;
import com.pet.walkthroughserver.modules.search.business.models.SearchResult;
import com.pet.walkthroughserver.modules.search.presentation.dto.SearchWalkthroughRequest;
import com.pet.walkthroughserver.modules.search.presentation.dto.SearchWalkthroughResponse;

@Component
public class SearchPresentationMapper {

    public SearchQuery toSearchQuery(SearchWalkthroughRequest request) {
        SearchQuery.SearchQueryBuilder builder = SearchQuery.builder()
                .query(request.getQuery())
                .page(request.getPage() != null ? request.getPage() : 0)
                .size(request.getSize() != null ? request.getSize() : 20)
                .sort(request.getSort() != null ? request.getSort() : "relevance");

        if (request.getFilters() != null) {
            builder.authorIds(request.getFilters().getAuthorIds())
                    .repository(request.getFilters().getRepository())
                    .statuses(request.getFilters().getStatus())
                    .createdFrom(request.getFilters().getCreatedFrom())
                    .createdTo(request.getFilters().getCreatedTo());
        }

        return builder.build();
    }

    public SearchWalkthroughResponse toResponse(SearchResult result) {
        List<SearchWalkthroughResponse.SearchHitResponse> hits = result.getHits().stream()
                .map(this::toHitResponse)
                .toList();

        Map<String, List<SearchWalkthroughResponse.FacetEntryResponse>> facets = result.getFacets() != null
                ? result.getFacets().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(f -> SearchWalkthroughResponse.FacetEntryResponse.builder()
                                        .value(f.getValue())
                                        .count(f.getCount())
                                        .build())
                                .toList()
                ))
                : Collections.emptyMap();

        return SearchWalkthroughResponse.builder()
                .total(result.getTotal())
                .page(result.getPage())
                .size(result.getSize())
                .hits(hits)
                .facets(facets)
                .build();
    }

    private SearchWalkthroughResponse.SearchHitResponse toHitResponse(SearchResult.SearchHit hit) {
        List<SearchWalkthroughResponse.ChapterHitResponse> chapterHits = hit.getChapterHits() != null
                ? hit.getChapterHits().stream()
                .map(ch -> SearchWalkthroughResponse.ChapterHitResponse.builder()
                        .chapterIndex(ch.getChapterIndex())
                        .field(ch.getField())
                        .snippet(ch.getSnippet())
                        .build())
                .toList()
                : Collections.emptyList();

        SearchWalkthroughResponse.HighlightsResponse highlights = SearchWalkthroughResponse.HighlightsResponse.builder()
                .title(hit.getHighlights() != null ? hit.getHighlights().get("title") : null)
                .description(hit.getHighlights() != null ? hit.getHighlights().get("description") : null)
                .chapterHits(chapterHits)
                .build();

        return SearchWalkthroughResponse.SearchHitResponse.builder()
                .id(hit.getDocument().getId())
                .title(hit.getDocument().getTitle())
                .authorName(hit.getDocument().getAuthorName())
                .repoFull(hit.getDocument().getRepoFull())
                .status(hit.getDocument().getStatus())
                .createdAt(hit.getDocument().getCreatedAt())
                .score(hit.getScore())
                .highlights(highlights)
                .build();
    }
}
