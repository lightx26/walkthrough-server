package com.pet.walkthroughserver.modules.profile.business.services;

import com.pet.walkthroughserver.modules.profile.business.models.PinnedWalkthrough;
import com.pet.walkthroughserver.modules.profile.presentation.dto.PinWalkthroughRequest;
import com.pet.walkthroughserver.modules.profile.presentation.dto.ReorderPinsRequest;

import java.util.List;
import java.util.UUID;

public interface WalkthroughPinService {

    List<PinnedWalkthrough> getPins(String username);

    PinnedWalkthrough pinWalkthrough(UUID userId, PinWalkthroughRequest request);

    void reorderPins(UUID userId, ReorderPinsRequest request);

    void unpinWalkthrough(UUID userId, UUID pinId);
}
