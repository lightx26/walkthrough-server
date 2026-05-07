package com.pet.walkthroughserver.modules.starredrepo.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StarRepoRequest {

    @NotBlank(message = "repoFullName is required")
    private String repoFullName;

    @NotBlank(message = "repoName is required")
    private String repoName;

    private String language;
}
