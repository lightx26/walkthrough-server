package com.pet.walkthroughserver.modules.github.presentation.mapper;

import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequestFile;
import com.pet.walkthroughserver.modules.github.presentation.dto.FileChangeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FileChangePresentationMapper {

    FileChangeResponse toResponse(GitHubPullRequestFile file);

    List<FileChangeResponse> toResponseList(List<GitHubPullRequestFile> files);
}
