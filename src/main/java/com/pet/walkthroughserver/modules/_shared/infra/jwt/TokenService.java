package com.pet.walkthroughserver.modules._shared.infra.jwt;

import java.time.Instant;
import java.util.Map;

/**
 * Service interface for JWT token operations.
 * Infrastructure concern for token generation and validation.
 */
public interface TokenService {

    String generateAccessToken(String userId, Map<String, Object> claims);

    String generateRefreshToken(String userId);

    String extractUserId(String token);

    Instant extractIssuedAt(String token);

    Instant extractExpiration(String token);

    String extractClaim(String token, String claimKey);

    boolean isValidToken(String token);
}
