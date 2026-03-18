package com.pet.walkthroughserver.modules.common.jwt;

import java.time.Instant;
import java.util.Map;

public interface TokenService {
    String generateAccessToken(String userId, Map<String, Object> claims);
    String generateRefreshToken(String userId);
    String extractUserId(String token);
    Instant extractIssuedAt(String token);
    Instant extractExpiration(String token);
    String extractClaim(String token, String claimKey);
    boolean isValidToken(String token);
}