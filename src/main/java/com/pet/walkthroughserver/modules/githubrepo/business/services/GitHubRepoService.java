package com.pet.walkthroughserver.modules.githubrepo.business.services;

import com.pet.walkthroughserver.modules._shared.dto.PageData;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubRepository;

import java.util.UUID;

public interface GitHubRepoService {

    PageData<GitHubRepository> getUserRepositories(UUID userId, int page, int perPage, String sort);

    PageData<GitHubRepository> searchRepositories(UUID userId, String query, int page, int perPage);

    GitHubRepository getRepository(UUID userId, String owner, String repo);
}
