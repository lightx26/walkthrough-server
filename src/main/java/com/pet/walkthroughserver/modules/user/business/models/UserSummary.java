package com.pet.walkthroughserver.modules.user.business.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable read projection of {@code UserEntity} for user-search results.
 *
 * <p>Deliberately excludes sensitive columns (e.g. the GitHub access token) and carries only the
 * fields the presentation layer exposes. Being free of JPA proxies, it serializes cleanly.
 */
public record UserSummary(
        UUID id,
        String username,
        String displayName,
        String email,
        String avatarUrl,
        Instant createdAt) {
}
