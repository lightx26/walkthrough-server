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
public class WalkthroughDocument {

    private UUID id;
    private UUID userId;
    private String authorName;
    private String owner;
    private String repo;
    private String repoFull;
    private Integer prNumber;
    private String title;
    private String description;
    private List<ChapterDocument> chapters;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterDocument {
        private String title;
        private String content;
    }
}
