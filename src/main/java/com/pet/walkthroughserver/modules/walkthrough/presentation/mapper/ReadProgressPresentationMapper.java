package com.pet.walkthroughserver.modules.walkthrough.presentation.mapper;

import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.ReadProgressResponse;
import com.pet.walkthroughserver.modules.walkthrough.repository.ReadProgressEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReadProgressPresentationMapper {

    ReadProgressResponse toResponse(ReadProgressEntity entity);
}
