package com.pet.walkthroughserver.modules._shared.infra.github;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class GitHubApiException extends AppException {

    public GitHubApiException(String message) {
        super(HttpStatus.BAD_GATEWAY, "GITHUB_API_ERROR", message);
    }
}
