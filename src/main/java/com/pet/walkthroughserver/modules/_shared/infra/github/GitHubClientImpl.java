package com.pet.walkthroughserver.modules._shared.infra.github;

import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubAccessTokenResponse;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubCommit;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequestFile;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubRepository;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubSearchReposResponse;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubUserInfo;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubApiException;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubAuthFailedException;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubClientImpl implements GitHubAuthClient, GitHubResourceClient {

    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String GITHUB_API_URL = "https://api.github.com";
    private static final String GITHUB_USER_API_URL = GITHUB_API_URL + "/user";

    private final RestClient restClient;

    @Value("${github.oauth.client-id}")
    private String clientId;

    @Value("${github.oauth.client-secret}")
    private String clientSecret;

    // ── GitHubAuthClient ──────────────────────────────────────────────

    @Override
    public GitHubAccessTokenResponse exchangeCodeForToken(String code) {
        record TokenRequest(String client_id, String client_secret, String code) {}

        GitHubAccessTokenResponse response = restClient.post()
                .uri(GITHUB_TOKEN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(new TokenRequest(clientId, clientSecret, code))
                .retrieve()
                .body(GitHubAccessTokenResponse.class);

        if (response == null || response.hasError()) {
            String errorMsg = response != null ? response.getErrorDescription() : "No response from GitHub";
            log.error("GitHub OAuth token exchange failed: {}", errorMsg);
            throw new GitHubAuthFailedException("GitHub authentication failed: " + errorMsg);
        }

        return response;
    }

    @Override
    public GitHubUserInfo fetchUserInfo(String accessToken) {
        GitHubUserInfo userInfo = restClient.get()
                .uri(GITHUB_USER_API_URL)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GitHubUserInfo.class);

        if (userInfo == null || userInfo.getId() == null) {
            throw new GitHubAuthFailedException("Failed to fetch GitHub user info");
        }

        return userInfo;
    }

    // ── GitHubResourceClient ──────────────────────────────────────────

    @Override
    public List<GitHubRepository> fetchUserRepositories(String accessToken, int page, int perPage, String sort) {
        List<GitHubRepository> repos = restClient.get()
                .uri(GITHUB_USER_API_URL + "/repos?page={page}&per_page={perPage}&sort={sort}&affiliation=owner,collaborator,organization_member",
                        page, perPage, sort)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (repos == null) {
            throw new GitHubApiException("Failed to fetch repositories from GitHub");
        }

        return repos;
    }

    @Override
    public GitHubSearchReposResponse searchRepositories(String accessToken, String query, int page, int perPage) {
        GitHubSearchReposResponse response = restClient.get()
                .uri(GITHUB_API_URL + "/search/repositories?q={query}&page={page}&per_page={perPage}&sort=updated",
                        query, page, perPage)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GitHubSearchReposResponse.class);

        if (response == null) {
            throw new GitHubApiException("Failed to search repositories on GitHub");
        }

        return response;
    }

    @Override
    public List<GitHubPullRequest> fetchPullRequests(String accessToken, String owner, String repo,
                                                      String state, int page, int perPage) {
        List<GitHubPullRequest> pullRequests = restClient.get()
                .uri(GITHUB_API_URL + "/repos/{owner}/{repo}/pulls?state={state}&page={page}&per_page={perPage}&sort=updated&direction=desc",
                        owner, repo, state, page, perPage)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (pullRequests == null) {
            throw new GitHubApiException("Failed to fetch pull requests from GitHub");
        }

        return pullRequests;
    }

    @Override
    public GitHubPullRequest fetchPullRequest(String accessToken, String owner, String repo, int pullNumber) {
        GitHubPullRequest pullRequest = restClient.get()
                .uri(GITHUB_API_URL + "/repos/{owner}/{repo}/pulls/{pullNumber}",
                        owner, repo, pullNumber)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GitHubPullRequest.class);

        if (pullRequest == null) {
            throw new GitHubResourceNotFoundException("Pull request not found");
        }

        return pullRequest;
    }

    @Override
    public List<GitHubCommit> fetchPullRequestCommits(String accessToken, String owner, String repo, int pullNumber) {
        List<GitHubCommit> commits = restClient.get()
                .uri(GITHUB_API_URL + "/repos/{owner}/{repo}/pulls/{pullNumber}/commits?per_page=100",
                        owner, repo, pullNumber)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (commits == null) {
            throw new GitHubApiException("Failed to fetch pull request commits from GitHub");
        }

        return commits;
    }

    @Override
    public List<GitHubPullRequestFile> fetchPullRequestFiles(String accessToken, String owner, String repo, int pullNumber) {
        List<GitHubPullRequestFile> files = restClient.get()
                .uri(GITHUB_API_URL + "/repos/{owner}/{repo}/pulls/{pullNumber}/files?per_page=100",
                        owner, repo, pullNumber)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (files == null) {
            throw new GitHubApiException("Failed to fetch pull request files from GitHub");
        }

        return files;
    }

    @Override
    public List<GitHubPullRequestFile> fetchCommitFiles(String accessToken, String owner, String repo, String commitSha) {
        record GitHubCommitDetail(List<GitHubPullRequestFile> files) {}

        GitHubCommitDetail detail = restClient.get()
                .uri(GITHUB_API_URL + "/repos/{owner}/{repo}/commits/{sha}",
                        owner, repo, commitSha)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GitHubCommitDetail.class);

        if (detail == null || detail.files() == null) {
            throw new GitHubApiException("Failed to fetch commit files from GitHub");
        }

        return detail.files();
    }

    @Override
    public List<GitHubPullRequest> searchUserPullRequests(String accessToken, String username, int perPage) {
        record SearchResponse(List<GitHubPullRequest> items) {}

        SearchResponse response = restClient.get()
                .uri(GITHUB_API_URL + "/search/issues?q=type:pr+author:{username}+sort:updated&per_page={perPage}",
                        username, perPage)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(SearchResponse.class);

        if (response == null || response.items() == null) {
            throw new GitHubApiException("Failed to search pull requests on GitHub");
        }

        return response.items();
    }

    @Override
    public Long createIssueComment(String accessToken, String owner, String repo, int issueNumber, String body) {
        record CommentRequest(String body) {}
        record CommentResponse(Long id) {}

        CommentResponse response = restClient.post()
                .uri(GITHUB_API_URL + "/repos/{owner}/{repo}/issues/{issueNumber}/comments",
                        owner, repo, issueNumber)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(new CommentRequest(body))
                .retrieve()
                .body(CommentResponse.class);

        if (response == null || response.id() == null) {
            throw new GitHubApiException("Failed to create issue comment on GitHub");
        }

        return response.id();
    }
}
