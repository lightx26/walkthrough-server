package com.pet.walkthroughserver.modules.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GitHubOAuthRequest {

    @NotBlank(message = "Authorization code is required")
    private String code;
}
