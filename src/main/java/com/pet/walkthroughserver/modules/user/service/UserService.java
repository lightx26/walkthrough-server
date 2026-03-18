package com.pet.walkthroughserver.modules.user.service;

import com.pet.walkthroughserver.modules.user.dto.UserResponse;
import com.pet.walkthroughserver.modules.user.entity.UserEntity;

import java.util.UUID;

public interface UserService {

    UserEntity findById(UUID id);

    UserEntity findOrCreateByGitHub(Long githubId, String username, String displayName,
                                     String email, String avatarUrl, String accessToken);

    UserResponse getCurrentUser(UUID userId);
}
