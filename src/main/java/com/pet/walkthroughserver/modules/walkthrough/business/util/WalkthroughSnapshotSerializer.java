package com.pet.walkthroughserver.modules.walkthrough.business.util;

import com.pet.walkthroughserver.modules.walkthrough.business.models.SnapshotContent;
import com.pet.walkthroughserver.modules.walkthrough.repository.AnnotationEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;

import java.util.List;

public final class WalkthroughSnapshotSerializer {

    private WalkthroughSnapshotSerializer() {}

    public static SnapshotContent serialize(WalkthroughEntity walkthrough) {
        List<SnapshotContent.SnapshotChapter> chapters = walkthrough.getChapters().stream()
                .map(WalkthroughSnapshotSerializer::serializeChapter)
                .toList();

        return new SnapshotContent(
                walkthrough.getTitle(),
                walkthrough.getDescription(),
                walkthrough.getOwner(),
                walkthrough.getRepo(),
                walkthrough.getPrNumber(),
                walkthrough.getCommitSha(),
                walkthrough.getVersion(),
                chapters);
    }

    private static SnapshotContent.SnapshotChapter serializeChapter(ChapterEntity chapter) {
        List<SnapshotContent.SnapshotFile> files = chapter.getFiles().stream()
                .map(WalkthroughSnapshotSerializer::serializeFile)
                .toList();

        return new SnapshotContent.SnapshotChapter(
                chapter.getId().toString(),
                chapter.getTitle(),
                chapter.getDescription(),
                chapter.getSortOrder(),
                files);
    }

    private static SnapshotContent.SnapshotFile serializeFile(WalkthroughFileEntity file) {
        List<SnapshotContent.SnapshotAnnotation> annotations = file.getAnnotations().stream()
                .map(WalkthroughSnapshotSerializer::serializeAnnotation)
                .toList();

        return new SnapshotContent.SnapshotFile(
                file.getId().toString(),
                file.getFilename(),
                file.getFileSha(),
                file.getFileStatus(),
                file.getSortOrder(),
                annotations);
    }

    private static SnapshotContent.SnapshotAnnotation serializeAnnotation(AnnotationEntity annotation) {
        return new SnapshotContent.SnapshotAnnotation(
                annotation.getId().toString(),
                annotation.getStartLine(),
                annotation.getEndLine(),
                annotation.getLineSide(),
                annotation.getContent(),
                annotation.getStatus().name());
    }
}
