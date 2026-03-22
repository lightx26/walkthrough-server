package com.pet.walkthroughserver.modules.user.dto;

import lombok.Builder;

/**
 * Data transfer object encapsulating GitHub user information
 * for user creation/update operations.
 */
@Builder
public record GitHubUserData(
        Long githubId,
        String username,
        String displayName,
        String email,
        String avatarUrl,
        String accessToken
) {
}
