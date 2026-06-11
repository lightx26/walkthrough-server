package com.pet.walkthroughserver.modules.walkthrough.business.models;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;

/**
 * Immutable read projection of {@code WalkthroughEntity} (with its chapter/file/annotation tree and
 * creator info) returned when viewing a single walkthrough.
 *
 * <p>The entity carries lazy associations ({@code user}, {@code chapters}) that must not leave the
 * business layer: serializing Hibernate proxies (e.g. into a Redis cache) fails. This flattened
 * projection holds exactly what {@code WalkthroughResponse} needs.
 */
public record WalkthroughDetail(
        UUID id,
        UUID userId,
        String creatorUsername,
        String creatorDisplayName,
        String creatorAvatarUrl,
        String title,
        String description,
        WalkthroughStatus status,
        String outdatedReason,
        String commitSha,
        String owner,
        String repo,
        Integer prNumber,
        List<WalkthroughChapter> chapters,
        Instant createdAt,
        Instant updatedAt) {
}
