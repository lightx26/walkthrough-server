package com.pet.walkthroughserver.modules.starredrepo.business.services;

import java.util.List;
import java.util.UUID;

import com.pet.walkthroughserver.modules.starredrepo.repository.StarredRepoEntity;

public interface StarredRepoService {

    StarredRepoEntity starRepo(UUID userId, String repoFullName, String repoName, String language);

    void unstarRepo(UUID userId, String repoFullName);

    List<StarredRepoEntity> getStarredRepos(UUID userId);

    boolean isStarred(UUID userId, String repoFullName);
}
