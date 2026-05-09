package com.pet.walkthroughserver.modules._shared.infra.github.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GitHubSearchPrsResponse {

    @JsonAlias("total_count")
    private int totalCount;

    @JsonAlias("incomplete_results")
    private boolean incompleteResults;

    private List<GitHubPullRequest> items;
}
