package com.pet.walkthroughserver.modules.search.infra;

import java.util.List;

import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.modules.search.business.models.WalkthroughDocument;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;

@Component
public class WalkthroughDocumentMapper {

    public WalkthroughDocument toDocument(WalkthroughEntity entity, String authorName) {
        List<WalkthroughDocument.ChapterDocument> chapters = entity.getChapters().stream()
                .map(this::toChapterDocument)
                .toList();

        return WalkthroughDocument.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .authorName(authorName)
                .owner(entity.getOwner())
                .repo(entity.getRepo())
                .repoFull(entity.getOwner() + "/" + entity.getRepo())
                .prNumber(entity.getPrNumber())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .chapters(chapters)
                .status(entity.getStatus().name())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private WalkthroughDocument.ChapterDocument toChapterDocument(ChapterEntity chapter) {
        return WalkthroughDocument.ChapterDocument.builder()
                .title(chapter.getTitle())
                .content(chapter.getDescription())
                .build();
    }
}
