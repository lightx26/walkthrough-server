package com.pet.walkthroughserver.modules.profile.business.models;

import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;

import java.time.Instant;
import java.util.UUID;

public record PinnedWalkthrough(
        UUID pinId,
        UUID walkthroughId,
        String title,
        String owner,
        String repo,
        int prNumber,
        WalkthroughStatus status,
        int sortOrder,
        Instant pinnedAt
) {}
