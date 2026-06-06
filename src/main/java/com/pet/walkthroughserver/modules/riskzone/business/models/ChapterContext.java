package com.pet.walkthroughserver.modules.riskzone.business.models;

import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;

import java.util.List;

/**
 * Read-only context surrounding a single file's diff, injected into the risk-detection
 * prompt so the LLM understands the chapter's intent and which sibling files participate
 * in the same change. Built defensively — every field is null-tolerant because chapter and
 * walkthrough descriptions are optional and the entity graph is loaded lazily.
 *
 * @param walkthroughTitle       PR/walkthrough title (may be null)
 * @param walkthroughDescription PR/walkthrough description (may be null/blank)
 * @param chapterTitle           chapter title (may be null)
 * @param chapterDescription     chapter description (may be null/blank)
 * @param siblingFiles           every file in the chapter (the manifest), never null
 */
public record ChapterContext(
        String walkthroughTitle,
        String walkthroughDescription,
        String chapterTitle,
        String chapterDescription,
        List<FileRef> siblingFiles
) {
    /** A single entry in the chapter file manifest. */
    public record FileRef(String filename, String status) {}

    /**
     * Builds a context snapshot from a chapter entity. Must be called inside the
     * persistence transaction (touches lazy associations). Null-safe throughout.
     */
    public static ChapterContext from(ChapterEntity chapter) {
        if (chapter == null) {
            return new ChapterContext(null, null, null, null, List.of());
        }

        WalkthroughEntity wt = chapter.getWalkthrough();
        List<FileRef> manifest = chapter.getFiles().stream()
                .map(f -> new FileRef(f.getFilename(), f.getFileStatus()))
                .toList();

        return new ChapterContext(
                wt != null ? wt.getTitle() : null,
                wt != null ? wt.getDescription() : null,
                chapter.getTitle(),
                chapter.getDescription(),
                manifest
        );
    }

    /** True when there is at least one descriptive field worth sending to the model. */
    public boolean hasMeaningfulContext() {
        return notBlank(walkthroughTitle) || notBlank(walkthroughDescription)
                || notBlank(chapterTitle) || notBlank(chapterDescription)
                || siblingFiles.size() > 1;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
