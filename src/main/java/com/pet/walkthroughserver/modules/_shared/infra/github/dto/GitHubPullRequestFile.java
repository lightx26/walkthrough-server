package com.pet.walkthroughserver.modules._shared.infra.github.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GitHubPullRequestFile {

    private String sha;

    private String filename;

    private String status;

    private int additions;

    private int deletions;

    private int changes;

    private String patch;

    @JsonAlias("previous_filename")
    private String previousFilename;
}
