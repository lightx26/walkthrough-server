package com.pet.walkthroughserver.modules.github.exceptions;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class GitHubAccessTokenNotFoundException extends AppException {

    public GitHubAccessTokenNotFoundException(String message) {
        super(HttpStatus.UNAUTHORIZED, "GITHUB_ACCESS_TOKEN_NOT_FOUND", message);
    }
}
