package com.pet.walkthroughserver.modules.auth.business.services;

import com.pet.walkthroughserver.modules._shared.infra.github.GitHubAuthClient;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubAccessTokenResponse;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubUserInfo;
import com.pet.walkthroughserver.modules._shared.infra.jwt.TokenService;
import com.pet.walkthroughserver.modules.auth.business.models.AuthResult;
import com.pet.walkthroughserver.modules.auth.exceptions.InvalidTokenException;
import com.pet.walkthroughserver.modules.auth.exceptions.TokenExpiredException;
import com.pet.walkthroughserver.modules.auth.repository.RefreshTokenEntity;
import com.pet.walkthroughserver.modules.auth.repository.RefreshTokenRepository;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private GitHubAuthClient gitHubAuthClient;

    @Mock
    private UserService userService;

    @Mock
    private TokenService tokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    // ── loginWithGitHub ───────────────────────────────────────────────

    @Test
    void loginWithGitHub_success_returnsAuthResult() {
        GitHubAccessTokenResponse tokenResponse = new GitHubAccessTokenResponse();
        tokenResponse.setAccessToken("gh-access-token");

        GitHubUserInfo userInfo = new GitHubUserInfo();
        userInfo.setId(12345L);
        userInfo.setLogin("testuser");
        userInfo.setName("Test User");
        userInfo.setEmail("test@example.com");
        userInfo.setAvatarUrl("https://avatar.url");

        UserEntity user = buildUserEntity();

        when(gitHubAuthClient.exchangeCodeForToken("code")).thenReturn(tokenResponse);
        when(gitHubAuthClient.fetchUserInfo("gh-access-token")).thenReturn(userInfo);
        when(userService.findOrCreateByGitHub(any())).thenReturn(user);
        when(tokenService.generateAccessToken(eq(USER_ID.toString()), any(Map.class))).thenReturn("jwt-access");
        when(tokenService.generateRefreshToken(USER_ID.toString())).thenReturn("jwt-refresh");
        when(tokenService.extractExpiration("jwt-refresh")).thenReturn(Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResult result = authService.loginWithGitHub("code");

        assertThat(result.getAccessToken()).isEqualTo("jwt-access");
        assertThat(result.getRefreshToken()).isEqualTo("jwt-refresh");
        assertThat(result.getUser()).isEqualTo(user);
        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
    }

    // ── refreshToken ──────────────────────────────────────────────────

    @Test
    void refreshToken_valid_returnsNewAuthResult() {
        UserEntity user = buildUserEntity();
        RefreshTokenEntity storedToken = RefreshTokenEntity.builder()
                .user(user)
                .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(tokenService.isValidToken("valid-token")).thenReturn(true);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString()))
                .thenReturn(Optional.of(storedToken));
        when(tokenService.generateAccessToken(eq(USER_ID.toString()), any(Map.class))).thenReturn("new-access");
        when(tokenService.generateRefreshToken(USER_ID.toString())).thenReturn("new-refresh");
        when(tokenService.extractExpiration("new-refresh")).thenReturn(Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResult result = authService.refreshToken("valid-token");

        assertThat(result.getAccessToken()).isEqualTo("new-access");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh");
        assertThat(storedToken.isRevoked()).isTrue();
    }

    @Test
    void refreshToken_invalidToken_throwsInvalidTokenException() {
        when(tokenService.isValidToken("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken("bad-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid or expired");
    }

    @Test
    void refreshToken_revokedOrNotFound_throwsInvalidTokenException() {
        when(tokenService.isValidToken("revoked-token")).thenReturn(true);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken("revoked-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("not found or revoked");
    }

    @Test
    void refreshToken_expired_throwsTokenExpiredException() {
        UserEntity user = buildUserEntity();
        RefreshTokenEntity expiredToken = RefreshTokenEntity.builder()
                .user(user)
                .tokenHash("hash")
                .expiresAt(Instant.now().minusSeconds(3600))
                .build();

        when(tokenService.isValidToken("expired-token")).thenReturn(true);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString()))
                .thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.refreshToken("expired-token"))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("expired");
    }

    // ── logout ────────────────────────────────────────────────────────

    @Test
    void logout_revokesAllTokensForUser() {
        authService.logout(USER_ID);

        verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
    }

    // ── me ────────────────────────────────────────────────────────────

    @Test
    void me_delegatesToUserService() {
        UserEntity user = buildUserEntity();
        when(userService.findById(USER_ID)).thenReturn(user);

        UserEntity result = authService.me(USER_ID);

        assertThat(result).isEqualTo(user);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private UserEntity buildUserEntity() {
        return UserEntity.builder()
                .id(USER_ID)
                .githubId(12345L)
                .username("testuser")
                .displayName("Test User")
                .avatarUrl("https://avatar.url")
                .build();
    }
}
