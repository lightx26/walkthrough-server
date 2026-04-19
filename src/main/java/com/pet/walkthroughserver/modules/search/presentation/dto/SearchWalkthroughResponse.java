package com.pet.walkthroughserver.modules.search.presentation.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchWalkthroughResponse {

    private long total;
    private int page;
    private int size;
    private List<SearchHitResponse> hits;
    private Map<String, List<FacetEntryResponse>> facets;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchHitResponse {
        private UUID id;
        private String title;
        private String authorName;
        private String repoFull;
        private String status;
        private Instant createdAt;
        private double score;
        private HighlightsResponse highlights;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HighlightsResponse {
        private List<String> title;
        private List<String> description;
        private List<ChapterHitResponse> chapterHits;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterHitResponse {
        private int chapterIndex;
        private String field;
        private String snippet;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacetEntryResponse {
        private String value;
        private long count;
    }
}
