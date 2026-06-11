package com.pet.walkthroughserver.modules.pinnedRepo.business.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.pet.walkthroughserver.modules.pinnedRepo.business.models.PinnedRepo;
import com.pet.walkthroughserver.modules.pinnedRepo.repository.PinnedRepoEntity;

/** Maps {@code PinnedRepoEntity} to its serialization-safe {@link PinnedRepo} read projection. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PinnedRepoProjectionMapper {

    PinnedRepo toPinnedRepo(PinnedRepoEntity entity);

    List<PinnedRepo> toPinnedRepos(List<PinnedRepoEntity> entities);
}
