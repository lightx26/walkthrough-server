package com.pet.walkthroughserver.modules.auth.presentation;

import com.pet.walkthroughserver.modules._shared.infra.cookie.CookieService;
import com.pet.walkthroughserver.modules._shared.infra.github.GitHubAuthFailedException;
import com.pet.walkthroughserver.modules._shared.infra.jwt.TokenService;
import com.pet.walkthroughserver.modules.auth.business.models.AuthResult;
import com.pet.walkthroughserver.modules.auth.business.services.AuthService;
import com.pet.walkthroughserver.modules.auth.exceptions.InvalidTokenException;
import com.pet.walkthroughserver.modules.auth.exceptions.TokenExpiredException;
import com.pet.walkthroughserver.modules.user.exceptions.UserNotFoundException;
import com.pet.walkthroughserver.modules.user.presentation.dto.UserResponse;
import com.pet.walkthroughserver.modules.user.presentation.mapper.UserPresentationMapper;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import com.pet.walkthroughserver.security.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthController.class, properties = "cors.allowed.origins=http://localhost")
class AuthControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CookieService cookieService;

    @MockitoBean
    private UserPresentationMapper userPresentationMapper;

    @MockitoBean
    private TokenService tokenService;

    // ── POST /v1/auth/github ──────────────────────────────────────────

    @Test
    void loginWithGitHub_success_returnsDataResponse() throws Exception {
        AuthResult authResult = buildAuthResult();
        when(authService.loginWithGitHub("test-code")).thenReturn(authResult);
        when(userPresentationMapper.toResponse(any(UserEntity.class))).thenReturn(buildUserResponse());

        mockMvc.perform(post("/v1/auth/github")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"test-code\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.id").value(USER_ID.toString()));

        verify(cookieService).setAuthCookies(any(), eq("access-token"), eq("refresh-token"));
    }

    @Test
    void loginWithGitHub_blankCode_returnsValidationError() throws Exception {
        mockMvc.perform(post("/v1/auth/github")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void loginWithGitHub_gitHubAuthFailed_returns401() throws Exception {
        when(authService.loginWithGitHub(anyString()))
                .thenThrow(new GitHubAuthFailedException("GitHub authentication failed"));

        mockMvc.perform(post("/v1/auth/github")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"bad-code\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("GITHUB_AUTH_FAILED"));
    }

    // ── POST /v1/auth/refresh ─────────────────────────────────────────

    @Test
    void refreshToken_success_returnsDataResponse() throws Exception {
        when(cookieService.extractRefreshToken(any())).thenReturn("valid-refresh-token");
        AuthResult authResult = buildAuthResult();
        when(authService.refreshToken("valid-refresh-token")).thenReturn(authResult);
        when(userPresentationMapper.toResponse(any(UserEntity.class))).thenReturn(buildUserResponse());

        mockMvc.perform(post("/v1/auth/refresh")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(cookieService).setAuthCookies(any(), eq("access-token"), eq("refresh-token"));
    }

    @Test
    void refreshToken_missingToken_returns401() throws Exception {
        when(cookieService.extractRefreshToken(any())).thenReturn(null);

        mockMvc.perform(post("/v1/auth/refresh")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("NOT_AUTHENTICATED"));
    }

    @Test
    void refreshToken_blankToken_returns401() throws Exception {
        when(cookieService.extractRefreshToken(any())).thenReturn("   ");

        mockMvc.perform(post("/v1/auth/refresh")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("NOT_AUTHENTICATED"));
    }

    @Test
    void refreshToken_invalidToken_returns401() throws Exception {
        when(cookieService.extractRefreshToken(any())).thenReturn("invalid-token");
        when(authService.refreshToken("invalid-token"))
                .thenThrow(new InvalidTokenException("Invalid or expired refresh token"));

        mockMvc.perform(post("/v1/auth/refresh")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
    }

    @Test
    void refreshToken_expiredToken_returns401() throws Exception {
        when(cookieService.extractRefreshToken(any())).thenReturn("expired-token");
        when(authService.refreshToken("expired-token"))
                .thenThrow(new TokenExpiredException("Refresh token expired"));

        mockMvc.perform(post("/v1/auth/refresh")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("TOKEN_EXPIRED"));
    }

    // ── GET /v1/auth/me ───────────────────────────────────────────────

    @Test
    void me_authenticated_returnsDataResponse() throws Exception {
        when(authService.me(USER_ID)).thenReturn(buildUserEntity());
        when(userPresentationMapper.toResponse(any(UserEntity.class))).thenReturn(buildUserResponse());

        mockMvc.perform(get("/v1/auth/me")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void me_notAuthenticated_returns401() throws Exception {
        mockMvc.perform(get("/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("NOT_AUTHENTICATED"));
    }

    @Test
    void me_userNotFound_returns404() throws Exception {
        when(authService.me(USER_ID)).thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(get("/v1/auth/me")
                        .with(authentication(authToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }

    // ── POST /v1/auth/logout ──────────────────────────────────────────

    @Test
    void logout_success_returnsDataResponse() throws Exception {
        mockMvc.perform(post("/v1/auth/logout")
                        .with(authentication(authToken()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService).logout(USER_ID);
        verify(cookieService).clearAuthCookies(any());
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private UsernamePasswordAuthenticationToken authToken() {
        AuthUser authUser = AuthUser.builder()
                .userId(USER_ID.toString())
                .username("testuser")
                .build();
        return new UsernamePasswordAuthenticationToken(authUser, null, List.of());
    }

    private AuthResult buildAuthResult() {
        return AuthResult.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .user(buildUserEntity())
                .build();
    }

    private UserEntity buildUserEntity() {
        return UserEntity.builder()
                .id(USER_ID)
                .githubId(12345L)
                .username("testuser")
                .displayName("Test User")
                .email("test@example.com")
                .avatarUrl("https://avatar.url")
                .createdAt(Instant.now())
                .build();
    }

    private UserResponse buildUserResponse() {
        return UserResponse.builder()
                .id(USER_ID)
                .username("testuser")
                .displayName("Test User")
                .email("test@example.com")
                .avatarUrl("https://avatar.url")
                .createdAt(Instant.now())
                .build();
    }
}
