package com.pet.walkthroughserver.modules.walkthrough.presentation.mapper;

import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.AnnotationResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.ChapterResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.WalkthroughFileResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.WalkthroughResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.WalkthroughSummaryResponse;
import com.pet.walkthroughserver.modules.walkthrough.repository.AnnotationEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WalkthroughPresentationMapper {

    @Mapping(target = "creatorUsername", source = "user.username")
    @Mapping(target = "creatorDisplayName", source = "user.displayName")
    @Mapping(target = "creatorAvatarUrl", source = "user.avatarUrl")
    WalkthroughResponse toResponse(WalkthroughEntity entity);

    @Mapping(target = "chapterCount", expression = "java(entity.getChapters() != null ? entity.getChapters().size() : 0)")
    @Mapping(target = "commentCount", ignore = true)
    WalkthroughSummaryResponse toSummaryResponse(WalkthroughEntity entity);

    ChapterResponse toChapterResponse(ChapterEntity entity);

    WalkthroughFileResponse toFileResponse(WalkthroughFileEntity entity);

    AnnotationResponse toAnnotationResponse(AnnotationEntity entity);

    List<WalkthroughSummaryResponse> toSummaryResponseList(List<WalkthroughEntity> entities);
}
