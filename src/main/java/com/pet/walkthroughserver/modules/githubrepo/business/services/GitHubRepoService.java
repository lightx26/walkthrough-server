package com.pet.walkthroughserver.modules.githubrepo.business.services;

import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubRepository;

import java.util.List;
import java.util.UUID;

public interface GitHubRepoService {

    List<GitHubRepository> getUserRepositories(UUID userId, int page, int perPage, String sort);

    List<GitHubRepository> searchRepositories(UUID userId, String query, int page, int perPage);

    GitHubRepository getRepository(UUID userId, String owner, String repo);
}
