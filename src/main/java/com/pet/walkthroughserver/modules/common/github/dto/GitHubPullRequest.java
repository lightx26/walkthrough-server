package com.pet.walkthroughserver.modules.common.github.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GitHubPullRequest {

    private Long id;

    private int number;

    private String title;

    private String state;

    private String body;

    @JsonAlias("html_url")
    private String htmlUrl;

    @JsonAlias("created_at")
    private String createdAt;

    @JsonAlias("updated_at")
    private String updatedAt;

    @JsonAlias("merged_at")
    private String mergedAt;

    @JsonAlias("changed_files")
    private int changedFiles;

    private int additions;

    private int deletions;

    private int commits;

    private GitHubUserInfo user;

    private Head head;

    private Head base;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Head {
        private String ref;
        private String sha;
        private String label;
    }
}
