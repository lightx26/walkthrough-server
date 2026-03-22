package com.pet.walkthroughserver.modules._shared.infra.jwt;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtTokenService implements TokenService {

    @Value("${jwt.expiry.access-token}")
    private long accessTokenExpirySeconds;

    @Value("${jwt.expiry.refresh-token}")
    private long refreshTokenExpirySeconds;

    @Value("${jwt.issuer}")
    private String issuer;

    private final SecretKey secretKey;
    private final JwtDecoder jwtDecoder;

    private String generateToken(String subject, long expirySeconds, Map<String, Object> extraClaims) {
        Instant now = Instant.now();

        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .signWith(secretKey);

        if (extraClaims != null && !extraClaims.isEmpty()) {
            builder.claims(extraClaims);
        }

        return builder.compact();
    }

    private String generateToken(String subject, long expirySeconds) {
        return generateToken(subject, expirySeconds, null);
    }

    @Override
    public String generateAccessToken(String userId, Map<String, Object> claims) {
        return generateToken(userId, accessTokenExpirySeconds, claims);
    }

    @Override
    public String generateRefreshToken(String userId) {
        return generateToken(userId, refreshTokenExpirySeconds);
    }

    @Override
    public String extractUserId(String token) {
        return jwtDecoder.decode(token).getSubject();
    }

    @Override
    public Instant extractIssuedAt(String token) {
        return jwtDecoder.decode(token).getIssuedAt();
    }

    @Override
    public Instant extractExpiration(String token) {
        return jwtDecoder.decode(token).getExpiresAt();
    }

    @Override
    public String extractClaim(String token, String claimKey) {
        return jwtDecoder.decode(token).getClaimAsString(claimKey);
    }

    @Override
    public boolean isValidToken(String token) {
        try {
            jwtDecoder.decode(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
