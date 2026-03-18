package com.pet.walkthroughserver.modules.common.github.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GitHubSearchReposResponse {

    @JsonAlias("total_count")
    private int totalCount;

    @JsonAlias("incomplete_results")
    private boolean incompleteResults;

    private List<GitHubRepository> items;
}
