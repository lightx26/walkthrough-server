package com.pet.walkthroughserver.modules.profile.business.services;

import com.pet.walkthroughserver.modules.profile.business.models.ProfileData;
import com.pet.walkthroughserver.modules.profile.business.models.ProfileStats;
import com.pet.walkthroughserver.modules.profile.business.models.ReviewingEntry;

import java.util.List;
import java.util.UUID;

public interface ProfileService {

    ProfileData getMyProfile(UUID userId);

    ProfileData getByUsername(String username);

    ProfileStats getStats(String username, UUID viewerId);

    List<ReviewingEntry> getReviewing(String username, UUID viewerId);
}
