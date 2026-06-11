package com.pet.walkthroughserver.modules.walkthrough.business.models;

import java.util.List;
import java.util.UUID;

/** Immutable read projection of {@code WalkthroughFileEntity} within a {@link WalkthroughDetail}. */
public record WalkthroughFile(
        UUID id,
        String filename,
        String fileSha,
        String fileStatus,
        Integer sortOrder,
        String rawPatch,
        List<WalkthroughAnnotation> annotations) {
}
