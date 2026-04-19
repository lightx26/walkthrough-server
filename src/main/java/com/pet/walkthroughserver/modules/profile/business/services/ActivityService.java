package com.pet.walkthroughserver.modules.profile.business.services;

import com.pet.walkthroughserver.modules._shared.dto.SliceData;
import com.pet.walkthroughserver.modules.profile.presentation.dto.ActivityEntryResponse;

import java.time.Instant;
import java.util.UUID;

public interface ActivityService {

    SliceData<ActivityEntryResponse> getActivity(String username, UUID viewerId, Instant before, int limit);
}
