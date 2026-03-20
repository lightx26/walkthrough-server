package com.pet.walkthroughserver.modules.auth.business.models;

import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResult {
    private String accessToken;
    private String refreshToken;
    private UserEntity user;
}
