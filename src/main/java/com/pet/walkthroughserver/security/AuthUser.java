package com.pet.walkthroughserver.security;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthUser {
    private String userId;
    private String username;
    private String firstName;
    private String lastName;
}
