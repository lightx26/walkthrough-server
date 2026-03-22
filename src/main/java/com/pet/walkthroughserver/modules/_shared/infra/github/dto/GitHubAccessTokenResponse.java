package com.pet.walkthroughserver.modules._shared.infra.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GitHubAccessTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    private String scope;

    private String error;

    @JsonProperty("error_description")
    private String errorDescription;

    public boolean hasError() {
        return error != null && !error.isBlank();
    }
}
