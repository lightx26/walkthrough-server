package com.pet.walkthroughserver.modules.template.presentation.mapper;

import com.pet.walkthroughserver.modules.template.presentation.dto.TemplateChapterResponse;
import com.pet.walkthroughserver.modules.template.presentation.dto.TemplateResponse;
import com.pet.walkthroughserver.modules.template.presentation.dto.TemplateSummaryResponse;
import com.pet.walkthroughserver.modules.template.repository.TemplateChapterEntity;
import com.pet.walkthroughserver.modules.template.repository.TemplateEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TemplatePresentationMapper {

    TemplateResponse toResponse(TemplateEntity entity);

    @Mapping(target = "chapterCount",
            expression = "java(entity.getChapters() != null ? entity.getChapters().size() : 0)")
    TemplateSummaryResponse toSummaryResponse(TemplateEntity entity);

    TemplateChapterResponse toChapterResponse(TemplateChapterEntity entity);

    List<TemplateResponse> toResponseList(List<TemplateEntity> entities);
}
