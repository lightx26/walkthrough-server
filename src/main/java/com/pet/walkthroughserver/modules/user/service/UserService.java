package com.pet.walkthroughserver.modules.user.service;

import com.pet.walkthroughserver.modules.user.dto.GitHubUserData;
import com.pet.walkthroughserver.modules.user.dto.UserResponse;
import com.pet.walkthroughserver.modules.user.entity.UserEntity;

import java.util.UUID;

public interface UserService {

    UserEntity findById(UUID id);

    /**
     * Finds an existing user by GitHub ID or creates a new one.
     *
     * @param githubUserData encapsulated GitHub user information
     * @return the found or newly created user entity
     */
    UserEntity findOrCreateByGitHub(GitHubUserData githubUserData);

    UserResponse getCurrentUser(UUID userId);
}
