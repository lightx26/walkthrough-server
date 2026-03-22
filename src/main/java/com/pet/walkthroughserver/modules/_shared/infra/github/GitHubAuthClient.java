package com.pet.walkthroughserver.modules._shared.infra.github;

import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubAccessTokenResponse;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubUserInfo;

/**
 * GitHub client for authentication-related operations.
 * Handles OAuth token exchange and user identity retrieval.
 */
public interface GitHubAuthClient {

    GitHubAccessTokenResponse exchangeCodeForToken(String code);

    GitHubUserInfo fetchUserInfo(String accessToken);
}
