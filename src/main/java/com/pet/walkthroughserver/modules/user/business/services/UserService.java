package com.pet.walkthroughserver.modules.user.business.services;

import com.pet.walkthroughserver.modules.user.business.models.GitHubUserData;
import com.pet.walkthroughserver.modules.user.business.models.UserSummary;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;

import java.util.List;
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

    UserEntity getCurrentUser(UUID userId);

    List<UserSummary> searchUsers(String query);
}
