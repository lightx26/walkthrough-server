package com.pet.walkthroughserver.modules.walkthrough.business.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.pet.walkthroughserver.modules.walkthrough.business.models.WalkthroughAnnotation;
import com.pet.walkthroughserver.modules.walkthrough.business.models.WalkthroughChapter;
import com.pet.walkthroughserver.modules.walkthrough.business.models.WalkthroughDetail;
import com.pet.walkthroughserver.modules.walkthrough.business.models.WalkthroughFile;
import com.pet.walkthroughserver.modules.walkthrough.business.models.WalkthroughSummary;
import com.pet.walkthroughserver.modules.walkthrough.repository.AnnotationEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;

/**
 * Maps {@code WalkthroughEntity} (and its chapter/file/annotation tree) to serialization-safe read
 * projections, so the JPA entity and its lazy associations never leave the business layer.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WalkthroughProjectionMapper {

    @Mapping(target = "creatorUsername", source = "user.username")
    @Mapping(target = "creatorDisplayName", source = "user.displayName")
    @Mapping(target = "creatorAvatarUrl", source = "user.avatarUrl")
    WalkthroughDetail toDetail(WalkthroughEntity entity);

    @Mapping(target = "chapterCount",
            expression = "java(entity.getChapters() != null ? entity.getChapters().size() : 0)")
    WalkthroughSummary toSummary(WalkthroughEntity entity);

    List<WalkthroughSummary> toSummaries(List<WalkthroughEntity> entities);

    WalkthroughChapter toChapter(ChapterEntity entity);

    WalkthroughFile toFile(WalkthroughFileEntity entity);

    WalkthroughAnnotation toAnnotation(AnnotationEntity entity);
}
