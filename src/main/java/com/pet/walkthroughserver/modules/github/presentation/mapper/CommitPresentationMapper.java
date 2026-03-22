package com.pet.walkthroughserver.modules.github.presentation.mapper;

import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubCommit;
import com.pet.walkthroughserver.modules.github.presentation.dto.CommitResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommitPresentationMapper {

    List<CommitResponse> toResponseList(List<GitHubCommit> commits);

    default CommitResponse toResponse(GitHubCommit commit) {
        if (commit == null) return null;

        CommitResponse.Author.AuthorBuilder authorBuilder = CommitResponse.Author.builder();
        if (commit.getCommit() != null && commit.getCommit().getAuthor() != null) {
            authorBuilder
                    .name(commit.getCommit().getAuthor().getName())
                    .email(commit.getCommit().getAuthor().getEmail());
        }
        if (commit.getAuthor() != null) {
            authorBuilder
                    .login(commit.getAuthor().getLogin())
                    .avatarUrl(commit.getAuthor().getAvatarUrl());
        }

        return CommitResponse.builder()
                .sha(commit.getSha())
                .message(commit.getCommit() != null ? commit.getCommit().getMessage() : null)
                .date(commit.getCommit() != null && commit.getCommit().getAuthor() != null
                        ? commit.getCommit().getAuthor().getDate() : null)
                .htmlUrl(commit.getHtmlUrl())
                .author(authorBuilder.build())
                .build();
    }
}
