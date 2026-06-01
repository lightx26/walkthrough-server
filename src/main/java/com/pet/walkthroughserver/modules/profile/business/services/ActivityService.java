package com.pet.walkthroughserver.modules.profile.business.services;

import com.pet.walkthroughserver.modules._shared.dto.SliceData;
import com.pet.walkthroughserver.modules.profile.repository.ActivityEntryEntity;

import java.time.Instant;
import java.util.UUID;

public interface ActivityService {

    SliceData<ActivityEntryEntity> getActivity(String username, UUID viewerId, Instant before, int limit);
}
