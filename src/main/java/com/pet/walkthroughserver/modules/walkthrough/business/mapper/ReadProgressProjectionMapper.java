package com.pet.walkthroughserver.modules.walkthrough.business.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.pet.walkthroughserver.modules.walkthrough.business.models.ReadProgress;
import com.pet.walkthroughserver.modules.walkthrough.repository.ReadProgressEntity;

/** Maps {@code ReadProgressEntity} to its serialization-safe {@link ReadProgress} read projection. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReadProgressProjectionMapper {

    ReadProgress toReadProgress(ReadProgressEntity entity);
}
