package com.pet.walkthroughserver.modules._shared.infra.cookie;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Service interface for managing HTTP cookies.
 * Infrastructure concern separated from business logic.
 */
public interface CookieService {

    String ACCESS_TOKEN_COOKIE = "access_token";
    String REFRESH_TOKEN_COOKIE = "refresh_token";

    void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken);

    void clearAuthCookies(HttpServletResponse response);

    String extractAccessToken(HttpServletRequest request);

    String extractRefreshToken(HttpServletRequest request);
}
