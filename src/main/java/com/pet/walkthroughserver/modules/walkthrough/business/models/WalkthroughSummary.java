package com.pet.walkthroughserver.modules.walkthrough.business.models;

import java.time.Instant;
import java.util.UUID;

import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;

/**
 * Immutable read projection of {@code WalkthroughEntity} for recent-walkthrough lists.
 *
 * <p>Carries the precomputed {@code chapterCount} instead of the lazy {@code chapters} collection,
 * so nothing JPA-bound leaks out of the business layer. The per-request {@code commentCount} is not
 * part of this projection and is enriched downstream.
 */
public record WalkthroughSummary(
        UUID id,
        UUID userId,
        String title,
        String description,
        String owner,
        String repo,
        Integer prNumber,
        WalkthroughStatus status,
        String outdatedReason,
        Integer chapterCount,
        Instant createdAt,
        Instant updatedAt) {
}
