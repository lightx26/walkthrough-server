package com.pet.walkthroughserver.modules.pinnedRepo.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PinnedRepoRequest {

    @NotBlank(message = "repoFullName is required")
    private String repoFullName;

    @NotBlank(message = "repoName is required")
    private String repoName;

    private String language;
}
