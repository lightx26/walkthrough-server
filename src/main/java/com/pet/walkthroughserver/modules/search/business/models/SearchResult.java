package com.pet.walkthroughserver.modules.search.business.models;

import java.util.List;
import java.util.Map;

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
public class SearchResult {

    private long total;
    private int page;
    private int size;
    private List<SearchHit> hits;
    private Map<String, List<FacetEntry>> facets;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchHit {
        private WalkthroughDocument document;
        private double score;
        private Map<String, List<String>> highlights;
        private List<ChapterHit> chapterHits;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterHit {
        private int chapterIndex;
        private String field;
        private String snippet;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacetEntry {
        private String value;
        private long count;
    }
}
