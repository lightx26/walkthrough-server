package com.pet.walkthroughserver.modules.pinnedRepo.business.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable read projection of {@code PinnedRepoEntity} returned by the service layer.
 *
 * <p>Carries only the fields the presentation layer needs and is free of JPA proxies and lazy
 * associations, so it serializes cleanly (e.g. when held in a Redis-backed cache).
 */
public record PinnedRepo(
        UUID id,
        String repoFullName,
        String repoName,
        String language,
        Instant createdAt) {
}
