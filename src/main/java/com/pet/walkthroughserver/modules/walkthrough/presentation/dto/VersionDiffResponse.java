package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VersionDiffResponse {

    private UUID walkthroughId;
    private Integer fromVersion;
    private Integer toVersion;
    private String fromCommitSha;
    private String toCommitSha;
    private List<ChapterDiff> chapters;
    private List<AnnotationDiff> outdatedAnnotations;

    @Getter
    @Builder
    public static class ChapterDiff {
        private String changeType; // ADDED, REMOVED, MODIFIED, UNCHANGED
        private String title;
        private String fromTitle;
        private String toTitle;
        private List<FileDiff> files;
    }

    @Getter
    @Builder
    public static class FileDiff {
        private String changeType; // ADDED, REMOVED, MODIFIED, UNCHANGED
        private String filename;
        private String fromFilename;
        private String toFilename;
    }

    @Getter
    @Builder
    public static class AnnotationDiff {
        private UUID annotationId;
        private String filename;
        private Integer startLine;
        private Integer endLine;
        private String lineSide;
        private String content;
        private String reason; // LINE_REMOVED, FILE_REMOVED, DIFF_CHANGED
    }
}
