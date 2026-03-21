package com.pet.walkthroughserver.modules.user.business.services;

import com.pet.walkthroughserver.modules.user.business.models.GitHubUserData;
import com.pet.walkthroughserver.modules.user.exceptions.UserNotFoundException;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import com.pet.walkthroughserver.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void findById_found_returnsUser() {
        UserEntity user = buildUserEntity();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserEntity result = userService.findById(USER_ID);

        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    void findById_notFound_throwsUserNotFoundException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(USER_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void findOrCreateByGitHub_existingUser_updatesAndReturns() {
        UserEntity existing = buildUserEntity();
        when(userRepository.findByGithubId(12345L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        GitHubUserData data = GitHubUserData.builder()
                .githubId(12345L)
                .username("updated-user")
                .displayName("Updated Name")
                .email("new@example.com")
                .avatarUrl("https://new-avatar.url")
                .accessToken("new-token")
                .build();

        UserEntity result = userService.findOrCreateByGitHub(data);

        assertThat(result.getUsername()).isEqualTo("updated-user");
        assertThat(result.getDisplayName()).isEqualTo("Updated Name");
        assertThat(result.getGithubAccessToken()).isEqualTo("new-token");
        verify(userRepository).save(existing);
    }

    @Test
    void findOrCreateByGitHub_newUser_createsAndReturns() {
        when(userRepository.findByGithubId(99999L)).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        GitHubUserData data = GitHubUserData.builder()
                .githubId(99999L)
                .username("newuser")
                .displayName("New User")
                .email("new@example.com")
                .avatarUrl("https://avatar.url")
                .accessToken("token")
                .build();

        UserEntity result = userService.findOrCreateByGitHub(data);

        assertThat(result.getGithubId()).isEqualTo(99999L);
        assertThat(result.getUsername()).isEqualTo("newuser");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void getCurrentUser_delegatesToFindById() {
        UserEntity user = buildUserEntity();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserEntity result = userService.getCurrentUser(USER_ID);

        assertThat(result).isEqualTo(user);
    }

    private UserEntity buildUserEntity() {
        return UserEntity.builder()
                .id(USER_ID)
                .githubId(12345L)
                .username("testuser")
                .displayName("Test User")
                .email("test@example.com")
                .avatarUrl("https://avatar.url")
                .githubAccessToken("gh-token")
                .build();
    }
}
