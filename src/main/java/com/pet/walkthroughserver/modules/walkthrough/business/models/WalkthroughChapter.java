package com.pet.walkthroughserver.modules.walkthrough.business.models;

import java.util.List;
import java.util.UUID;

/** Immutable read projection of {@code ChapterEntity} within a {@link WalkthroughDetail}. */
public record WalkthroughChapter(
        UUID id,
        String title,
        String description,
        Integer sortOrder,
        List<WalkthroughFile> files) {
}
