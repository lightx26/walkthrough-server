package com.pet.walkthroughserver.modules.walkthrough.business.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable read projection of {@code ReadProgressEntity}.
 *
 * <p>The per-request {@code readChapterIds} list is not part of this projection; it is enriched
 * downstream by the presentation assembler.
 */
public record ReadProgress(
        UUID id,
        UUID userId,
        UUID walkthroughId,
        UUID lastChapterId,
        Integer readChapters,
        Integer totalChapters,
        Integer timeSpentSec,
        Instant readAt) {
}
