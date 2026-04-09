package com.pet.walkthroughserver.modules.githubpr.presentation.mapper;

import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubUserInfo;
import com.pet.walkthroughserver.modules.githubpr.presentation.dto.PullRequestResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PullRequestPresentationMapper {

    @Mapping(source = "user", target = "author")
    PullRequestResponse toResponse(GitHubPullRequest pr);

    List<PullRequestResponse> toResponseList(List<GitHubPullRequest> prs);

    default PullRequestResponse.Author toAuthor(GitHubUserInfo user) {
        if (user == null) return null;
        return PullRequestResponse.Author.builder()
                .id(user.getId())
                .login(user.getLogin())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    default PullRequestResponse.Branch toBranch(GitHubPullRequest.Head head) {
        if (head == null) return null;
        return PullRequestResponse.Branch.builder()
                .ref(head.getRef())
                .sha(head.getSha())
                .build();
    }
}
