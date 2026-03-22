package com.pet.walkthroughserver.exceptionHandlers;

import com.pet.walkthroughserver.interceptors.ErrorResponse;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubAuthFailedException;
import com.pet.walkthroughserver.modules.auth.exceptions.InvalidTokenException;
import com.pet.walkthroughserver.modules.auth.exceptions.NotAuthenticatedException;
import com.pet.walkthroughserver.modules.auth.exceptions.TokenExpiredException;
import com.pet.walkthroughserver.modules.auth.presentation.AuthController;
import com.pet.walkthroughserver.modules.user.exceptions.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.pet.walkthroughserver.utils.ExceptionResponse.respond;

@Slf4j
@Order(1)
@RestControllerAdvice(basePackageClasses = AuthController.class)
public class AuthExceptionHandler {

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        log.warn("InvalidTokenException: {}", ex.getMessage());
        return respond(ex);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpired(TokenExpiredException ex) {
        log.warn("TokenExpiredException: {}", ex.getMessage());
        return respond(ex);
    }

    @ExceptionHandler(NotAuthenticatedException.class)
    public ResponseEntity<ErrorResponse> handleNotAuthenticated(NotAuthenticatedException ex) {
        log.warn("NotAuthenticatedException: {}", ex.getMessage());
        return respond(ex);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        log.warn("UserNotFoundException during auth: {}", ex.getMessage());
        return respond(ex);
    }

    @ExceptionHandler(GitHubAuthFailedException.class)
    public ResponseEntity<ErrorResponse> handleGitHubAuthFailed(GitHubAuthFailedException ex) {
        log.warn("GitHubAuthFailedException during auth: {}", ex.getMessage());
        return respond(ex);
    }
}
