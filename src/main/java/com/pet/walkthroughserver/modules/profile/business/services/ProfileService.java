package com.pet.walkthroughserver.modules.profile.business.services;

import com.pet.walkthroughserver.modules.profile.presentation.dto.ProfileResponse;
import com.pet.walkthroughserver.modules.profile.presentation.dto.ProfileStatsResponse;

import java.util.UUID;

public interface ProfileService {

    ProfileResponse getMyProfile(UUID userId);

    ProfileResponse getByUsername(String username);

    ProfileStatsResponse getStats(String username, UUID viewerId);
}
