package com.pet.walkthroughserver.modules.comment.business.services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pet.walkthroughserver.modules._shared.infra.github.GitHubResourceClient;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubAccessTokenNotFoundException;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GitHubCommentServiceImpl implements GitHubCommentService {

    private final GitHubResourceClient gitHubResourceClient;
    private final UserService userService;

    @Override
    public Long createPrComment(UUID userId, String owner, String repo, int prNumber, String body) {
        String accessToken = getGitHubAccessToken(userId);
        return gitHubResourceClient.createIssueComment(accessToken, owner, repo, prNumber, body);
    }

    @Override
    public Long createPrReviewComment(UUID userId, String owner, String repo, int prNumber,
                                       String body, String commitId, String path, int position) {
        String accessToken = getGitHubAccessToken(userId);
        return gitHubResourceClient.createPullReviewComment(accessToken, owner, repo, prNumber, body, commitId, path, position);
    }

    private String getGitHubAccessToken(UUID userId) {
        UserEntity user = userService.findById(userId);
        String token = user.getGithubAccessToken();
        if (token == null || token.isBlank()) {
            throw new GitHubAccessTokenNotFoundException("GitHub access token not found. Please re-authenticate.");
        }
        return token;
    }
}
