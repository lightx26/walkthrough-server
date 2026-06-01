package com.pet.walkthroughserver.modules.walkthrough.presentation.assembler;

import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.modules.walkthrough.business.models.StalenessResult;
import com.pet.walkthroughserver.modules.walkthrough.business.models.VersionDiff;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.StalenessResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.VersionDiffResponse;

@Component
public class VersionDiffAssembler {

    public StalenessResponse toResponse(StalenessResult model) {
        return StalenessResponse.builder()
                .stale(model.stale())
                .currentCommitSha(model.currentCommitSha())
                .latestCommitSha(model.latestCommitSha())
                .currentVersion(model.currentVersion())
                .build();
    }

    public VersionDiffResponse toResponse(VersionDiff model) {
        return VersionDiffResponse.builder()
                .walkthroughId(model.walkthroughId())
                .fromVersion(model.fromVersion())
                .toVersion(model.toVersion())
                .fromCommitSha(model.fromCommitSha())
                .toCommitSha(model.toCommitSha())
                .chapters(model.chapters().stream().map(this::toChapterDiff).toList())
                .outdatedAnnotations(model.outdatedAnnotations().stream().map(this::toAnnotationDiff).toList())
                .build();
    }

    private VersionDiffResponse.ChapterDiff toChapterDiff(VersionDiff.ChapterDiff ch) {
        return VersionDiffResponse.ChapterDiff.builder()
                .changeType(ch.changeType())
                .title(ch.title())
                .fromTitle(ch.fromTitle())
                .toTitle(ch.toTitle())
                .files(ch.files().stream().map(this::toFileDiff).toList())
                .build();
    }

    private VersionDiffResponse.FileDiff toFileDiff(VersionDiff.FileDiff f) {
        return VersionDiffResponse.FileDiff.builder()
                .changeType(f.changeType())
                .filename(f.filename())
                .fromFilename(f.fromFilename())
                .toFilename(f.toFilename())
                .build();
    }

    private VersionDiffResponse.AnnotationDiff toAnnotationDiff(VersionDiff.AnnotationDiff a) {
        return VersionDiffResponse.AnnotationDiff.builder()
                .annotationId(a.annotationId())
                .filename(a.filename())
                .startLine(a.startLine())
                .endLine(a.endLine())
                .lineSide(a.lineSide())
                .content(a.content())
                .reason(a.reason())
                .build();
    }
}
