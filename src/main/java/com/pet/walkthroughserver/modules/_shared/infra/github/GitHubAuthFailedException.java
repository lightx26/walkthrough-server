package com.pet.walkthroughserver.modules._shared.infra.github;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class GitHubAuthFailedException extends AppException {

    public GitHubAuthFailedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "GITHUB_AUTH_FAILED", message);
    }
}
