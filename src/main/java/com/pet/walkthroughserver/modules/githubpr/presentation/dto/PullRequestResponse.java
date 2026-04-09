package com.pet.walkthroughserver.modules.githubpr.presentation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PullRequestResponse {

    private Long id;
    private int number;
    private String title;
    private String state;
    private String body;
    private String htmlUrl;
    private String createdAt;
    private String updatedAt;
    private String mergedAt;
    private int changedFiles;
    private int additions;
    private int deletions;
    private int commits;
    private Author author;
    private Branch head;
    private Branch base;

    @Getter
    @Builder
    public static class Author {
        private Long id;
        private String login;
        private String avatarUrl;
    }

    @Getter
    @Builder
    public static class Branch {
        private String ref;
        private String sha;
        private String label;
    }
}
