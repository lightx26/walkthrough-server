package com.pet.walkthroughserver.modules.search.presentation.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchWalkthroughRequest {

    private String query;
    private SearchFilters filters;
    private Integer page;
    private Integer size;
    private String sort;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchFilters {
        private List<UUID> authorIds;
        private String repository;
        private List<String> status;
        private Instant createdFrom;
        private Instant createdTo;
    }
}
