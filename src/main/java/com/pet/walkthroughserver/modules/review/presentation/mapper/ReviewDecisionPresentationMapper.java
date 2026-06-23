package com.pet.walkthroughserver.modules.review.presentation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.pet.walkthroughserver.modules.review.presentation.dto.ReviewDecisionResponse;
import com.pet.walkthroughserver.modules.review.repository.ReviewDecisionEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReviewDecisionPresentationMapper {

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.avatarUrl", target = "avatarUrl")
    @Mapping(target = "decision", expression = "java(entity.getDecision().name())")
    ReviewDecisionResponse toResponse(ReviewDecisionEntity entity);
}
