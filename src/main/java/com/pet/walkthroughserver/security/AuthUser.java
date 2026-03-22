package com.pet.walkthroughserver.security;

import lombok.Builder;
import lombok.Getter;

/**
 * Represents the authenticated user principal extracted from JWT token.
 * Fields correspond to claims stored in the access token.
 */
@Getter
@Builder
public class AuthUser {
    private String userId;
    private String username;
    private String displayName;
    private String avatarUrl;
}
