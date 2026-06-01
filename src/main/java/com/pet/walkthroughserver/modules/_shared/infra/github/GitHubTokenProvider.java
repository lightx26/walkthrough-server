package com.pet.walkthroughserver.modules._shared.infra.github;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubAccessTokenNotFoundException;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;

import lombok.RequiredArgsConstructor;

/**
 * Single source of truth for resolving a user's GitHub access token.
 *
 * <p>Replaces the {@code getGitHubAccessToken}/{@code getAccessTokenFromUser} logic that was
 * previously copy-pasted across {@code GitHubPrServiceImpl}, {@code GitHubRepoServiceImpl} and
 * {@code GitHubCommentServiceImpl}.
 */
@Component
@RequiredArgsConstructor
public class GitHubTokenProvider {

    private final UserService userService;

    /** Resolve and validate the GitHub access token for the given user id. */
    public String accessTokenFor(UUID userId) {
        return requireToken(userService.findById(userId));
    }

    /**
     * Validate and return the GitHub access token from an already-loaded user. Use this when the
     * caller already needs the {@link UserEntity} (e.g. for the username) to avoid a second lookup.
     */
    public String requireToken(UserEntity user) {
        String token = user.getGithubAccessToken();
        if (token == null || token.isBlank()) {
            throw new GitHubAccessTokenNotFoundException("GitHub access token not found. Please re-authenticate.");
        }
        return token;
    }
}
