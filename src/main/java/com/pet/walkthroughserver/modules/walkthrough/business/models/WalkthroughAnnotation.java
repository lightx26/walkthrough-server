package com.pet.walkthroughserver.modules.walkthrough.business.models;

import java.util.UUID;

/** Immutable read projection of {@code AnnotationEntity} within a {@link WalkthroughDetail}. */
public record WalkthroughAnnotation(
        UUID id,
        Integer startLine,
        Integer endLine,
        String lineSide,
        String content,
        Integer sortOrder) {
}
