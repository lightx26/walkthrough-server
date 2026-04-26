package com.pet.walkthroughserver.modules.walkthrough.business.services;

import java.util.UUID;

import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.StalenessResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.VersionDiffResponse;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;

public interface WalkthroughVersionService {

    StalenessResponse checkStaleness(UUID userId, UUID walkthroughId);

    WalkthroughEntity createNewVersion(UUID userId, UUID walkthroughId);

    VersionDiffResponse getVersionDiff(UUID userId, UUID walkthroughId, int fromVersion, int toVersion);
}
