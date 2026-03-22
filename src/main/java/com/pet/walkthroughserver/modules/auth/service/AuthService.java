package com.pet.walkthroughserver.modules.auth.service;

import com.pet.walkthroughserver.modules.auth.dto.AuthResponse;
import com.pet.walkthroughserver.modules.user.dto.UserResponse;

import java.util.UUID;

public interface AuthService {

    AuthResponse loginWithGitHub(String code);

    AuthResponse refreshToken(String refreshToken);

    void logout(UUID userId);

    UserResponse me(UUID userId);
}
