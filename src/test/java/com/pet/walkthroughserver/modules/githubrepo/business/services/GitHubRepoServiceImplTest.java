package com.pet.walkthroughserver.modules.githubrepo.business.services;

import com.pet.walkthroughserver.modules._shared.dto.PageData;
import com.pet.walkthroughserver.modules._shared.infra.github.GitHubResourceClient;
import com.pet.walkthroughserver.modules._shared.infra.github.GitHubTokenProvider;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPagedResult;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubRepository;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubSearchReposResponse;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubRepoServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private GitHubResourceClient gitHubResourceClient;

    @Mock
    private GitHubTokenProvider gitHubTokenProvider;

    @Mock
    private UserService userService;

    @InjectMocks
    private GitHubRepoServiceImpl gitHubRepoService;

    @Test
    void getUserRepositories_success_returnsPage() {
        GitHubRepository repo = new GitHubRepository();
        repo.setName("my-repo");

        when(gitHubTokenProvider.accessTokenFor(USER_ID)).thenReturn("gh-token");
        when(gitHubResourceClient.fetchUserRepositories("gh-token", 1, 30, "updated"))
                .thenReturn(GitHubPagedResult.of(List.of(repo), 1, 1, 1L));

        PageData<GitHubRepository> result = gitHubRepoService.getUserRepositories(USER_ID, 1, 30, "updated");

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getName()).isEqualTo("my-repo");
    }

    @Test
    void searchRepositories_success_scopesQueryWithUsername() {
        UserEntity user = buildUser("gh-token");
        GitHubRepository repo = new GitHubRepository();
        repo.setName("search-result");

        GitHubSearchReposResponse searchResponse = new GitHubSearchReposResponse();
        searchResponse.setItems(List.of(repo));
        searchResponse.setTotalCount(1);

        when(userService.findById(USER_ID)).thenReturn(user);
        when(gitHubTokenProvider.requireToken(user)).thenReturn("gh-token");
        when(gitHubResourceClient.searchRepositories("gh-token", "test user:testuser", 1, 30))
                .thenReturn(searchResponse);

        PageData<GitHubRepository> result = gitHubRepoService.searchRepositories(USER_ID, "test", 1, 30);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getName()).isEqualTo("search-result");
    }

    private UserEntity buildUser(String token) {
        return UserEntity.builder()
                .id(USER_ID)
                .githubId(12345L)
                .username("testuser")
                .displayName("Test User")
                .githubAccessToken(token)
                .build();
    }
}
