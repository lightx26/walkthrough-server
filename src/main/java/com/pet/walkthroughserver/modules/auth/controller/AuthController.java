package com.pet.walkthroughserver.modules.auth.controller;

import com.pet.walkthroughserver.modules.auth.dto.AuthResponse;
import com.pet.walkthroughserver.modules.auth.dto.GitHubOAuthRequest;
import com.pet.walkthroughserver.modules.auth.dto.RefreshTokenRequest;
import com.pet.walkthroughserver.modules.auth.service.AuthService;
import com.pet.walkthroughserver.modules.common.dto.ApiResponse;
import com.pet.walkthroughserver.security.AuthUser;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PermitAll
    @PostMapping("/github")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGitHub(
            @Valid @RequestBody GitHubOAuthRequest request) {
        AuthResponse response = authService.loginWithGitHub(request.getCode());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PermitAll
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal AuthUser authUser) {
        authService.logout(UUID.fromString(authUser.getUserId()));
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }
}
