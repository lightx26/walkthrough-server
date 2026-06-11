package com.pet.walkthroughserver.modules.user.presentation.mapper;

import com.pet.walkthroughserver.modules.user.business.models.UserSummary;
import com.pet.walkthroughserver.modules.user.presentation.dto.UserResponse;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserPresentationMapper {

    UserResponse toResponse(UserEntity entity);

    UserResponse toResponse(UserSummary summary);
}
