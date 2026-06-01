package com.pet.walkthroughserver.modules.comment.presentation.mapper;

import com.pet.walkthroughserver.modules.comment.presentation.dto.CommentResponse;
import com.pet.walkthroughserver.modules.comment.repository.CommentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommentPresentationMapper {

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.avatarUrl", target = "avatarUrl")
    @Mapping(target = "replies", ignore = true)
    // Keep the lowercase wire value ("pending"/"synced"/...) rather than the enum name.
    @Mapping(target = "syncStatus",
            expression = "java(entity.getSyncStatus() == null ? null : entity.getSyncStatus().dbValue())")
    CommentResponse toResponse(CommentEntity entity);

    List<CommentResponse> toResponseList(List<CommentEntity> entities);
}
