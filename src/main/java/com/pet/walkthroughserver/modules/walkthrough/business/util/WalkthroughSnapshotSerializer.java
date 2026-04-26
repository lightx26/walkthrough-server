package com.pet.walkthroughserver.modules.walkthrough.business.util;

import com.pet.walkthroughserver.modules.walkthrough.repository.AnnotationEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WalkthroughSnapshotSerializer {

    private WalkthroughSnapshotSerializer() {}

    public static Map<String, Object> serialize(WalkthroughEntity walkthrough) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("title", walkthrough.getTitle());
        snapshot.put("description", walkthrough.getDescription());
        snapshot.put("owner", walkthrough.getOwner());
        snapshot.put("repo", walkthrough.getRepo());
        snapshot.put("prNumber", walkthrough.getPrNumber());
        snapshot.put("commitSha", walkthrough.getCommitSha());
        snapshot.put("version", walkthrough.getVersion());

        List<Map<String, Object>> chapters = walkthrough.getChapters().stream()
                .map(WalkthroughSnapshotSerializer::serializeChapter)
                .toList();
        snapshot.put("chapters", chapters);

        return snapshot;
    }

    private static Map<String, Object> serializeChapter(ChapterEntity chapter) {
        Map<String, Object> ch = new LinkedHashMap<>();
        ch.put("id", chapter.getId().toString());
        ch.put("title", chapter.getTitle());
        ch.put("description", chapter.getDescription());
        ch.put("sortOrder", chapter.getSortOrder());

        List<Map<String, Object>> files = chapter.getFiles().stream()
                .map(WalkthroughSnapshotSerializer::serializeFile)
                .toList();
        ch.put("files", files);

        return ch;
    }

    private static Map<String, Object> serializeFile(WalkthroughFileEntity file) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("id", file.getId().toString());
        f.put("filename", file.getFilename());
        f.put("fileSha", file.getFileSha());
        f.put("fileStatus", file.getFileStatus());
        f.put("sortOrder", file.getSortOrder());

        List<Map<String, Object>> annotations = file.getAnnotations().stream()
                .map(WalkthroughSnapshotSerializer::serializeAnnotation)
                .toList();
        f.put("annotations", annotations);

        return f;
    }

    private static Map<String, Object> serializeAnnotation(AnnotationEntity annotation) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("id", annotation.getId().toString());
        a.put("startLine", annotation.getStartLine());
        a.put("endLine", annotation.getEndLine());
        a.put("lineSide", annotation.getLineSide());
        a.put("content", annotation.getContent());
        a.put("status", annotation.getStatus().name());
        return a;
    }
}
