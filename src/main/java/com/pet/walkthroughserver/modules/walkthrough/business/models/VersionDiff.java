package com.pet.walkthroughserver.modules.walkthrough.business.models;

import java.util.List;
import java.util.UUID;

public record VersionDiff(
        UUID walkthroughId,
        int fromVersion,
        int toVersion,
        String fromCommitSha,
        String toCommitSha,
        List<ChapterDiff> chapters,
        List<AnnotationDiff> outdatedAnnotations
) {
    public record ChapterDiff(
            String changeType,
            String title,
            String fromTitle,
            String toTitle,
            List<FileDiff> files
    ) {}

    public record FileDiff(
            String changeType,
            String filename,
            String fromFilename,
            String toFilename
    ) {}

    public record AnnotationDiff(
            UUID annotationId,
            String filename,
            int startLine,
            int endLine,
            String lineSide,
            String content,
            String reason
    ) {}
}
