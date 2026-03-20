package com.pet.walkthroughserver.modules.auth.business.services;

import com.pet.walkthroughserver.modules.auth.business.models.AuthResult;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;

import java.util.UUID;

public interface AuthService {

    AuthResult loginWithGitHub(String code);

    AuthResult refreshToken(String refreshToken);

    void logout(UUID userId);

    UserEntity me(UUID userId);
}
