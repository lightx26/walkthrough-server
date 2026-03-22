package com.pet.walkthroughserver.security;

import com.pet.walkthroughserver.modules._shared.infra.cookie.CookieService;
import com.pet.walkthroughserver.modules._shared.infra.jwt.TokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final CookieService cookieService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            try {
                if (tokenService.isValidToken(token)) {
                    String userId = tokenService.extractUserId(token);
                    String username = tokenService.extractClaim(token, "username");
                    String displayName = tokenService.extractClaim(token, "displayName");
                    String avatarUrl = tokenService.extractClaim(token, "avatarUrl");

                    AuthUser authUser = AuthUser.builder()
                            .userId(userId)
                            .username(username)
                            .displayName(displayName)
                            .avatarUrl(avatarUrl)
                            .build();

                    var auth = new UsernamePasswordAuthenticationToken(
                            authUser, null, Collections.emptyList());

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // Primary: httpOnly cookie
        String cookieToken = cookieService.extractAccessToken(request);
        if (cookieToken != null && !cookieToken.isBlank()) {
            return cookieToken;
        }
        // Fallback: Authorization header (useful for API clients / testing)
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length());
        }
        return null;
    }
}
