package com.pet.walkthroughserver.modules.walkthrough.business.services;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.pet.walkthroughserver.modules.walkthrough.business.models.WalkthroughDetail;
import com.pet.walkthroughserver.modules.walkthrough.business.models.WalkthroughSummary;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.CreateWalkthroughRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.UpdateWalkthroughRequest;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;

public interface WalkthroughService {

    WalkthroughEntity create(UUID userId, String username, CreateWalkthroughRequest request);

    List<WalkthroughEntity> listByPr(UUID requestingUserId, String owner, String repo, Integer prNumber);

    List<WalkthroughSummary> listRecent(UUID userId);

    WalkthroughDetail getById(UUID id, UUID requestingUserId);

    WalkthroughEntity syncCheck(UUID requestingUserId, UUID walkthroughId);

    WalkthroughEntity update(UUID userId, UUID walkthroughId, UpdateWalkthroughRequest request);

    void delete(UUID userId, UUID walkthroughId);

    long countByRepo(String owner, String repo, UUID requestingUserId);

    long countNonDraftByRepo(String owner, String repo);

    long countByPr(String owner, String repo, int prNumber, UUID requestingUserId);

    Map<String, Long> countByRepos(List<String> repoFullNames, UUID requestingUserId);

    Map<Integer, Long> countByPrs(String owner, String repo, List<Integer> prNumbers, UUID requestingUserId);

    Map<UUID, Long> getCommentCounts(List<UUID> walkthroughIds);
}
