package com.pet.walkthroughserver.modules.github.business.services;

import com.pet.walkthroughserver.modules._shared.infra.github.GitHubResourceClient;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubRepository;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubSearchReposResponse;
import com.pet.walkthroughserver.modules.github.exceptions.GitHubAccessTokenNotFoundException;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private GitHubResourceClient gitHubResourceClient;

    @Mock
    private UserService userService;

    @InjectMocks
    private GitHubServiceImpl gitHubService;

    @Test
    void getUserRepositories_success_returnsList() {
        UserEntity user = buildUserWithToken("gh-token");
        GitHubRepository repo = new GitHubRepository();
        repo.setName("my-repo");

        when(userService.findById(USER_ID)).thenReturn(user);
        when(gitHubResourceClient.fetchUserRepositories("gh-token", 1, 30, "updated"))
                .thenReturn(List.of(repo));

        List<GitHubRepository> result = gitHubService.getUserRepositories(USER_ID, 1, 30, "updated");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("my-repo");
    }

    @Test
    void searchRepositories_success_scopesQueryWithUsername() {
        UserEntity user = buildUserWithToken("gh-token");
        GitHubRepository repo = new GitHubRepository();
        repo.setName("search-result");

        GitHubSearchReposResponse searchResponse = new GitHubSearchReposResponse();
        searchResponse.setItems(List.of(repo));

        when(userService.findById(USER_ID)).thenReturn(user);
        when(gitHubResourceClient.searchRepositories("gh-token", "test user:testuser", 1, 30))
                .thenReturn(searchResponse);

        List<GitHubRepository> result = gitHubService.searchRepositories(USER_ID, "test", 1, 30);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("search-result");
    }

    @Test
    void getPullRequests_success_returnsList() {
        UserEntity user = buildUserWithToken("gh-token");
        GitHubPullRequest pr = new GitHubPullRequest();
        pr.setTitle("Fix bug");

        when(userService.findById(USER_ID)).thenReturn(user);
        when(gitHubResourceClient.fetchPullRequests("gh-token", "owner", "repo", "open", 1, 30))
                .thenReturn(List.of(pr));

        List<GitHubPullRequest> result = gitHubService.getPullRequests(USER_ID, "owner", "repo", "open", 1, 30);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTitle()).isEqualTo("Fix bug");
    }

    @Test
    void getPullRequest_success_returnsPr() {
        UserEntity user = buildUserWithToken("gh-token");
        GitHubPullRequest pr = new GitHubPullRequest();
        pr.setNumber(42);
        pr.setTitle("Add feature");

        when(userService.findById(USER_ID)).thenReturn(user);
        when(gitHubResourceClient.fetchPullRequest("gh-token", "owner", "repo", 42))
                .thenReturn(pr);

        GitHubPullRequest result = gitHubService.getPullRequest(USER_ID, "owner", "repo", 42);

        assertThat(result.getTitle()).isEqualTo("Add feature");
    }

    @Test
    void getUserRepositories_noAccessToken_throwsException() {
        UserEntity user = buildUserWithToken(null);
        when(userService.findById(USER_ID)).thenReturn(user);

        assertThatThrownBy(() -> gitHubService.getUserRepositories(USER_ID, 1, 30, "updated"))
                .isInstanceOf(GitHubAccessTokenNotFoundException.class)
                .hasMessageContaining("access token not found");
    }

    @Test
    void getUserRepositories_blankAccessToken_throwsException() {
        UserEntity user = buildUserWithToken("   ");
        when(userService.findById(USER_ID)).thenReturn(user);

        assertThatThrownBy(() -> gitHubService.getUserRepositories(USER_ID, 1, 30, "updated"))
                .isInstanceOf(GitHubAccessTokenNotFoundException.class);
    }

    private UserEntity buildUserWithToken(String token) {
        return UserEntity.builder()
                .id(USER_ID)
                .githubId(12345L)
                .username("testuser")
                .displayName("Test User")
                .githubAccessToken(token)
                .build();
    }
}
