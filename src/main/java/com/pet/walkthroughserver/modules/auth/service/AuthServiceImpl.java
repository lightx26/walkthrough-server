package com.pet.walkthroughserver.modules.auth.service;

import com.pet.walkthroughserver.exceptions.AppException;
import com.pet.walkthroughserver.modules.auth.dto.AuthResponse;
import com.pet.walkthroughserver.modules.auth.entity.RefreshTokenEntity;
import com.pet.walkthroughserver.modules.auth.repository.RefreshTokenRepository;
import com.pet.walkthroughserver.modules.common.github.GitHubClient;
import com.pet.walkthroughserver.modules.common.github.dto.GitHubAccessTokenResponse;
import com.pet.walkthroughserver.modules.common.github.dto.GitHubUserInfo;
import com.pet.walkthroughserver.modules.common.jwt.TokenService;
import com.pet.walkthroughserver.modules.user.entity.UserEntity;
import com.pet.walkthroughserver.modules.user.mapper.UserMapper;
import com.pet.walkthroughserver.modules.user.service.UserService;
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

    private final GitHubClient gitHubClient;
    private final UserService userService;
    private final TokenService tokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AuthResponse loginWithGitHub(String code) {
        GitHubAccessTokenResponse tokenResponse = gitHubClient.exchangeCodeForToken(code);
        GitHubUserInfo userInfo = gitHubClient.fetchUserInfo(tokenResponse.getAccessToken());

        UserEntity user = userService.findOrCreateByGitHub(
                userInfo.getId(),
                userInfo.getLogin(),
                userInfo.getName(),
                userInfo.getEmail(),
                userInfo.getAvatarUrl(),
                tokenResponse.getAccessToken()
        );

        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenService.isValidToken(refreshToken)) {
            throw AppException.unauthorized("Invalid or expired refresh token");
        }

        String tokenHash = hashToken(refreshToken);
        RefreshTokenEntity storedToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> AppException.unauthorized("Refresh token not found or revoked"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw AppException.unauthorized("Refresh token expired");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        UserEntity user = storedToken.getUser();
        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private AuthResponse generateAuthResponse(UserEntity user) {
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

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userMapper.toResponse(user))
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
