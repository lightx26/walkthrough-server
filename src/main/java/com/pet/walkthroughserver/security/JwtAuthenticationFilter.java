package com.pet.walkthroughserver.security;

import com.pet.walkthroughserver.modules.common.jwt.TokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());
            try {
                if (tokenService.isValidToken(token)) {
                    String userId = tokenService.extractUserId(token);
                    String username = tokenService.extractClaim(token, "username");
                    String firstName = tokenService.extractClaim(token, "firstName");
                    String lastName = tokenService.extractClaim(token, "lastName");

                    AuthUser authUser = AuthUser.builder()
                            .userId(userId)
                            .username(username)
                            .firstName(firstName)
                            .lastName(lastName)
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
}


