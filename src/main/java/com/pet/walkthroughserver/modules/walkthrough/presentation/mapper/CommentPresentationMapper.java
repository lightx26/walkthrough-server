package com.pet.walkthroughserver.modules.walkthrough.presentation.mapper;

import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.CommentResponse;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughCommentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommentPresentationMapper {

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.avatarUrl", target = "avatarUrl")
    CommentResponse toResponse(WalkthroughCommentEntity entity);

    List<CommentResponse> toResponseList(List<WalkthroughCommentEntity> entities);
}
