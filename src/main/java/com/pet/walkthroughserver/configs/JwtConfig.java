package com.pet.walkthroughserver.configs;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;

@Configuration
public class JwtConfig {
    @Value("${jwt.secret.key}")
    private String secret;

    @Bean
    public SecretKey secretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey secretKey) {
        MacAlgorithm macAlgorithm = switch (secretKey.getAlgorithm()) {
            case "HmacSHA256" -> MacAlgorithm.HS256;
            case "HmacSHA384" -> MacAlgorithm.HS384;
            case "HmacSHA512" -> MacAlgorithm.HS512;
            default -> throw new IllegalArgumentException("Invalid secret key length: " + secretKey.getEncoded().length);
        };

        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(macAlgorithm)
                .build();
    }
}
