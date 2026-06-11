package com.pet.walkthroughserver.modules.pinnedRepo.business.services;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.pet.walkthroughserver.modules.pinnedRepo.business.models.PinnedRepo;
import com.pet.walkthroughserver.modules.pinnedRepo.repository.PinnedRepoEntity;

public interface PinnedRepoService {

    PinnedRepoEntity pinRepo(UUID userId, String repoFullName, String repoName, String language);

    void unpinRepo(UUID userId, String repoFullName);

    List<PinnedRepo> getPinnedRepos(UUID userId);

    boolean isPinned(UUID userId, String repoFullName);

    Set<String> findPinnedFullNames(UUID userId, List<String> repoFullNames);
}
