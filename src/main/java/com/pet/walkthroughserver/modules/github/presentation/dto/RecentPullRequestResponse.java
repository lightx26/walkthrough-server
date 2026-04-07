package com.pet.walkthroughserver.modules.github.presentation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecentPullRequestResponse {

    private Long id;
    private int number;
    private String title;
    private String state;
    private String htmlUrl;
    private String createdAt;
    private String updatedAt;
    private String owner;
    private String repo;
    private Author author;

    @Getter
    @Builder
    public static class Author {
        private String login;
        private String avatarUrl;
    }
}
