package com.pet.walkthroughserver.modules.walkthrough.business.models;

import java.util.List;

public record SnapshotContent(
        String title,
        String description,
        String owner,
        String repo,
        Integer prNumber,
        String commitSha,
        Integer version,
        List<SnapshotChapter> chapters
) {
    public record SnapshotChapter(
            String id,
            String title,
            String description,
            Integer sortOrder,
            List<SnapshotFile> files
    ) {}

    public record SnapshotFile(
            String id,
            String filename,
            String fileSha,
            String fileStatus,
            Integer sortOrder,
            List<SnapshotAnnotation> annotations
    ) {}

    public record SnapshotAnnotation(
            String id,
            Integer startLine,
            Integer endLine,
            String lineSide,
            String content,
            String status
    ) {}
}
