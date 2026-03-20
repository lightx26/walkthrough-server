package com.pet.walkthroughserver.modules.auth.presentation;

import com.pet.walkthroughserver.modules._shared.dto.DataResponse;
import com.pet.walkthroughserver.modules.auth.exceptions.NotAuthenticatedException;
import com.pet.walkthroughserver.modules._shared.infra.cookie.CookieService;
import com.pet.walkthroughserver.modules.auth.business.models.AuthResult;
import com.pet.walkthroughserver.modules.auth.business.services.AuthService;
import com.pet.walkthroughserver.modules.auth.presentation.dto.GitHubOAuthRequest;
import com.pet.walkthroughserver.modules.user.presentation.dto.UserResponse;
import com.pet.walkthroughserver.modules.user.presentation.mapper.UserPresentationMapper;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
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
    private final UserPresentationMapper userPresentationMapper;

    @PermitAll
    @PostMapping("/github")
    public ResponseEntity<DataResponse<UserResponse>> loginWithGitHub(
            @Valid @RequestBody GitHubOAuthRequest request,
            HttpServletResponse response) {
        AuthResult authResult = authService.loginWithGitHub(request.getCode());
        cookieService.setAuthCookies(response, authResult.getAccessToken(), authResult.getRefreshToken());
        return ResponseEntity.ok(DataResponse.of(userPresentationMapper.toResponse(authResult.getUser())));
    }

    @PermitAll
    @PostMapping("/refresh")
    public ResponseEntity<DataResponse<UserResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = cookieService.extractRefreshToken(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new NotAuthenticatedException("Refresh token not found");
        }
        AuthResult authResult = authService.refreshToken(refreshToken);
        cookieService.setAuthCookies(response, authResult.getAccessToken(), authResult.getRefreshToken());
        return ResponseEntity.ok(DataResponse.of(userPresentationMapper.toResponse(authResult.getUser())));
    }

    @PermitAll
    @GetMapping("/me")
    public ResponseEntity<DataResponse<UserResponse>> me(@AuthenticationPrincipal AuthUser authUser) {
        if (authUser == null) {
            throw new NotAuthenticatedException("Not authenticated");
        }
        UserEntity user = authService.me(UUID.fromString(authUser.getUserId()));
        return ResponseEntity.ok(DataResponse.of(userPresentationMapper.toResponse(user)));
    }

    @PostMapping("/logout")
    public ResponseEntity<DataResponse<Void>> logout(
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletResponse response) {
        authService.logout(UUID.fromString(authUser.getUserId()));
        cookieService.clearAuthCookies(response);
        return ResponseEntity.ok(DataResponse.of("Logged out successfully", null));
    }
}
