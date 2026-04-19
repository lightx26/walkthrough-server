package com.pet.walkthroughserver.modules.search.business.models;

import java.time.Instant;
import java.util.List;
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
public class SearchQuery {

    private String query;
    private List<UUID> authorIds;
    private String repository;
    private List<String> statuses;
    private Instant createdFrom;
    private Instant createdTo;
    private int page;
    private int size;
    private String sort;
}
