package com.pet.walkthroughserver.modules.user.mapper;

import com.pet.walkthroughserver.modules.user.dto.UserResponse;
import com.pet.walkthroughserver.modules.user.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    UserResponse toResponse(UserEntity entity);
}
