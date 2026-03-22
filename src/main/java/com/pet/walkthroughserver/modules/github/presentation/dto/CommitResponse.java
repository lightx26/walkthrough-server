package com.pet.walkthroughserver.modules.github.presentation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommitResponse {

    private String sha;
    private String message;
    private String date;
    private String htmlUrl;
    private Author author;

    @Getter
    @Builder
    public static class Author {
        private String name;
        private String email;
        private String login;
        private String avatarUrl;
    }
}
