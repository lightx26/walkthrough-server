package com.pet.walkthroughserver.modules.githubpr.business.services;

import com.pet.walkthroughserver.modules._shared.infra.github.GitHubResourceClient;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubAccessTokenNotFoundException;
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
class GitHubPrServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private GitHubResourceClient gitHubResourceClient;

    @Mock
    private UserService userService;

    @InjectMocks
    private GitHubPrServiceImpl gitHubPrService;

    @Test
    void getPullRequests_success_returnsList() {
        UserEntity user = buildUserWithToken("gh-token");
        GitHubPullRequest pr = new GitHubPullRequest();
        pr.setTitle("Fix bug");

        when(userService.findById(USER_ID)).thenReturn(user);
        when(gitHubResourceClient.fetchPullRequests("gh-token", "owner", "repo", "open", 1, 30))
                .thenReturn(List.of(pr));

        List<GitHubPullRequest> result = gitHubPrService.getPullRequests(USER_ID, "owner", "repo", "open", 1, 30);

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

        GitHubPullRequest result = gitHubPrService.getPullRequest(USER_ID, "owner", "repo", 42);

        assertThat(result.getTitle()).isEqualTo("Add feature");
    }

    @Test
    void getPullRequests_noAccessToken_throwsException() {
        UserEntity user = buildUserWithToken(null);
        when(userService.findById(USER_ID)).thenReturn(user);

        assertThatThrownBy(() -> gitHubPrService.getPullRequests(USER_ID, "owner", "repo", "open", 1, 30))
                .isInstanceOf(GitHubAccessTokenNotFoundException.class)
                .hasMessageContaining("access token not found");
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
