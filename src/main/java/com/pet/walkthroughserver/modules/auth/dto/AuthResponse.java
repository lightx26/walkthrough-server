package com.pet.walkthroughserver.modules.auth.dto;

import com.pet.walkthroughserver.modules.user.dto.UserResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private UserResponse user;
}
