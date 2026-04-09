package com.pet.walkthroughserver.modules.githubrepo.presentation.mapper;

import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubRepository;
import com.pet.walkthroughserver.modules.githubrepo.presentation.dto.RepositoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RepositoryPresentationMapper {

    @Mapping(source = "private", target = "isPrivate")
    RepositoryResponse toResponse(GitHubRepository repo);

    List<RepositoryResponse> toResponseList(List<GitHubRepository> repos);

    default RepositoryResponse.Owner toOwner(GitHubRepository.GitHubOwner owner) {
        if (owner == null) return null;
        return RepositoryResponse.Owner.builder()
                .id(owner.getId())
                .login(owner.getLogin())
                .avatarUrl(owner.getAvatarUrl())
                .build();
    }
}
