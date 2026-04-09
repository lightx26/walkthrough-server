package com.pet.walkthroughserver.exceptionHandlers;

import com.pet.walkthroughserver.interceptors.ErrorResponse;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubApiException;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubAuthFailedException;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubResourceNotFoundException;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubAccessTokenNotFoundException;
import com.pet.walkthroughserver.modules.github.presentation.GitHubController;
import com.pet.walkthroughserver.modules.user.exceptions.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.pet.walkthroughserver.utils.ExceptionResponse.respond;

@Slf4j
@Order(1)
@RestControllerAdvice(basePackageClasses = GitHubController.class)
public class GitHubExceptionHandler {

    @ExceptionHandler(GitHubAccessTokenNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccessTokenNotFound(GitHubAccessTokenNotFoundException ex) {
        log.warn("GitHubAccessTokenNotFoundException: {}", ex.getMessage());
        return respond(ex);
    }

    @ExceptionHandler(GitHubAuthFailedException.class)
    public ResponseEntity<ErrorResponse> handleGitHubAuthFailed(GitHubAuthFailedException ex) {
        log.warn("GitHubAuthFailedException: {}", ex.getMessage());
        return respond(ex);
    }

    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<ErrorResponse> handleGitHubApiError(GitHubApiException ex) {
        log.warn("GitHubApiException: {}", ex.getMessage());
        return respond(ex);
    }

    @ExceptionHandler(GitHubResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGitHubResourceNotFound(GitHubResourceNotFoundException ex) {
        log.warn("GitHubResourceNotFoundException: {}", ex.getMessage());
        return respond(ex);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        log.warn("UserNotFoundException in GitHub context: {}", ex.getMessage());
        return respond(ex);
    }
}
