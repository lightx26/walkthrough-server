package com.pet.walkthroughserver.modules.walkthrough.business.services;

import java.util.UUID;

import com.pet.walkthroughserver.modules.walkthrough.business.models.StalenessResult;
import com.pet.walkthroughserver.modules.walkthrough.business.models.VersionDiff;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;

public interface WalkthroughVersionService {

    StalenessResult checkStaleness(UUID userId, UUID walkthroughId);

    WalkthroughEntity createNewVersion(UUID userId, UUID walkthroughId);

    VersionDiff getVersionDiff(UUID userId, UUID walkthroughId, int fromVersion, int toVersion);
}
