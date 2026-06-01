package com.pet.walkthroughserver.modules._shared.infra.github;

import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubAccessTokenNotFoundException;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubTokenProviderTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private UserService userService;

    @InjectMocks
    private GitHubTokenProvider gitHubTokenProvider;

    @Test
    void accessTokenFor_present_returnsToken() {
        when(userService.findById(USER_ID)).thenReturn(buildUser("gh-token"));

        assertThat(gitHubTokenProvider.accessTokenFor(USER_ID)).isEqualTo("gh-token");
    }

    @Test
    void accessTokenFor_missing_throws() {
        when(userService.findById(USER_ID)).thenReturn(buildUser(null));

        assertThatThrownBy(() -> gitHubTokenProvider.accessTokenFor(USER_ID))
                .isInstanceOf(GitHubAccessTokenNotFoundException.class)
                .hasMessageContaining("access token not found");
    }

    @Test
    void accessTokenFor_blank_throws() {
        when(userService.findById(USER_ID)).thenReturn(buildUser("   "));

        assertThatThrownBy(() -> gitHubTokenProvider.accessTokenFor(USER_ID))
                .isInstanceOf(GitHubAccessTokenNotFoundException.class);
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
