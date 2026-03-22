package com.pet.walkthroughserver.modules.auth.controller;

import com.pet.walkthroughserver.exceptions.AppException;
import com.pet.walkthroughserver.modules._shared.infra.cookie.CookieService;
import com.pet.walkthroughserver.modules._shared.dto.ApiResponse;
import com.pet.walkthroughserver.modules.auth.dto.AuthResponse;
import com.pet.walkthroughserver.modules.auth.dto.GitHubOAuthRequest;
import com.pet.walkthroughserver.modules.auth.service.AuthService;
import com.pet.walkthroughserver.modules.user.dto.UserResponse;
import com.pet.walkthroughserver.security.AuthUser;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;

    @PermitAll
    @PostMapping("/github")
    public ResponseEntity<ApiResponse<UserResponse>> loginWithGitHub(
            @Valid @RequestBody GitHubOAuthRequest request,
            HttpServletResponse response) {
        AuthResponse authResult = authService.loginWithGitHub(request.getCode());
        cookieService.setAuthCookies(response, authResult.getAccessToken(), authResult.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(authResult.getUser()));
    }

    @PermitAll
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<UserResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = cookieService.extractRefreshToken(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw AppException.unauthorized("Refresh token not found");
        }
        AuthResponse authResult = authService.refreshToken(refreshToken);
        cookieService.setAuthCookies(response, authResult.getAccessToken(), authResult.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(authResult.getUser()));
    }

    @PermitAll
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal AuthUser authUser) {
        if (authUser == null) {
            throw AppException.unauthorized("Not authenticated");
        }
        UserResponse user = authService.me(UUID.fromString(authUser.getUserId()));
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletResponse response) {
        authService.logout(UUID.fromString(authUser.getUserId()));
        cookieService.clearAuthCookies(response);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }
}
