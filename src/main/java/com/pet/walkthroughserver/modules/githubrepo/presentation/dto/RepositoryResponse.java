package com.pet.walkthroughserver.modules.githubrepo.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RepositoryResponse {

    private Long id;
    private String name;
    private String fullName;
    private String description;

    @JsonProperty("isPrivate")
    private boolean isPrivate;

    private String htmlUrl;
    private String language;
    private int stargazersCount;
    private int forksCount;
    private int openIssuesCount;
    private String updatedAt;
    private Owner owner;

    @Getter
    @Builder
    public static class Owner {
        private Long id;
        private String login;
        private String avatarUrl;
    }
}
