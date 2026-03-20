package com.pet.walkthroughserver.modules.auth.business.services;

import com.pet.walkthroughserver.modules.auth.exceptions.InvalidTokenException;
import com.pet.walkthroughserver.modules.auth.exceptions.TokenExpiredException;
import com.pet.walkthroughserver.modules._shared.infra.github.GitHubAuthClient;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubAccessTokenResponse;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubUserInfo;
import com.pet.walkthroughserver.modules._shared.infra.jwt.TokenService;
import com.pet.walkthroughserver.modules.auth.business.models.AuthResult;
import com.pet.walkthroughserver.modules.auth.repository.RefreshTokenEntity;
import com.pet.walkthroughserver.modules.auth.repository.RefreshTokenRepository;
import com.pet.walkthroughserver.modules.user.business.models.GitHubUserData;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final GitHubAuthClient gitHubAuthClient;
    private final UserService userService;
    private final TokenService tokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public AuthResult loginWithGitHub(String code) {
        GitHubAccessTokenResponse tokenResponse = gitHubAuthClient.exchangeCodeForToken(code);
        GitHubUserInfo userInfo = gitHubAuthClient.fetchUserInfo(tokenResponse.getAccessToken());

        GitHubUserData githubUserData = GitHubUserData.builder()
                .githubId(userInfo.getId())
                .username(userInfo.getLogin())
                .displayName(userInfo.getName())
                .email(userInfo.getEmail())
                .avatarUrl(userInfo.getAvatarUrl())
                .accessToken(tokenResponse.getAccessToken())
                .build();

        UserEntity user = userService.findOrCreateByGitHub(githubUserData);

        return generateAuthResult(user);
    }

    @Override
    @Transactional
    public AuthResult refreshToken(String refreshToken) {
        if (!tokenService.isValidToken(refreshToken)) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        String tokenHash = hashToken(refreshToken);
        RefreshTokenEntity storedToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found or revoked"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Refresh token expired");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        UserEntity user = storedToken.getUser();
        return generateAuthResult(user);
    }

    @Override
    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Override
    public UserEntity me(UUID userId) {
        return userService.findById(userId);
    }

    private AuthResult generateAuthResult(UserEntity user) {
        Map<String, Object> claims = Map.of(
                "username", user.getUsername(),
                "displayName", user.getDisplayName() != null ? user.getDisplayName() : "",
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
        );

        String userId = user.getId().toString();
        String accessToken = tokenService.generateAccessToken(userId, claims);
        String refreshToken = tokenService.generateRefreshToken(userId);

        Instant expiration = tokenService.extractExpiration(refreshToken);

        RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.builder()
                .user(user)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(expiration)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(user)
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
