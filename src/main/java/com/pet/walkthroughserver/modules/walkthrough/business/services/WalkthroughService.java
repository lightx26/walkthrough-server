package com.pet.walkthroughserver.modules.walkthrough.business.services;

import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.CreateWalkthroughRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.UpdateWalkthroughRequest;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;

import java.util.List;
import java.util.UUID;

public interface WalkthroughService {

    WalkthroughEntity create(UUID userId, String username, CreateWalkthroughRequest request);

    List<WalkthroughEntity> listByPr(String owner, String repo, Integer prNumber);

    WalkthroughEntity getById(UUID id);

    WalkthroughEntity update(UUID userId, UUID walkthroughId, UpdateWalkthroughRequest request);

    void delete(UUID userId, UUID walkthroughId);
}
