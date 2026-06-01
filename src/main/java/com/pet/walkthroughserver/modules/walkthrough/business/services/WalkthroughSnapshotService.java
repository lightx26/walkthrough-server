package com.pet.walkthroughserver.modules.walkthrough.business.services;

import com.pet.walkthroughserver.modules.walkthrough.business.models.SnapshotContent;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;

import java.util.UUID;

public interface WalkthroughSnapshotService {

    void captureSnapshot(WalkthroughEntity walkthrough);

    SnapshotContent getSnapshotContent(UUID walkthroughId, int version, WalkthroughEntity walkthrough);
}
