package com.pet.walkthroughserver.modules.common.github.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GitHubRepository {

    private Long id;

    private String name;

    @JsonAlias("full_name")
    private String fullName;

    private String description;

    // Lombok generates isPrivate() for boolean fields named "isPrivate".
    // Jackson strips the "is" prefix → serializes as "private" by default.
    // @JsonProperty("isPrivate") forces serialization key to "isPrivate".
    // @JsonAlias("private") lets deserialization accept GitHub's "private" key.
    @JsonProperty("isPrivate")
    @JsonAlias("private")
    private boolean isPrivate;

    @JsonAlias("html_url")
    private String htmlUrl;

    private String language;

    @JsonAlias("stargazers_count")
    private int stargazersCount;

    @JsonAlias("forks_count")
    private int forksCount;

    @JsonAlias("open_issues_count")
    private int openIssuesCount;

    @JsonAlias("updated_at")
    private String updatedAt;

    private GitHubOwner owner;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class GitHubOwner {
        private Long id;
        private String login;
        @JsonAlias("avatar_url")
        private String avatarUrl;
    }
}
