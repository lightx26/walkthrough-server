package com.pet.walkthroughserver.modules.user.business.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.pet.walkthroughserver.modules.user.business.models.UserSummary;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;

/** Maps {@code UserEntity} to its serialization-safe {@link UserSummary} read projection. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserProjectionMapper {

    UserSummary toUserSummary(UserEntity entity);

    List<UserSummary> toUserSummaries(List<UserEntity> entities);
}
