package com.pet.walkthroughserver.modules.profile.business.services;

import com.pet.walkthroughserver.modules._shared.dto.SliceData;
import com.pet.walkthroughserver.modules.profile.repository.ActivityEntryEntity;
import com.pet.walkthroughserver.modules.profile.repository.ActivityEntryRepository;
import com.pet.walkthroughserver.modules.user.exceptions.UserNotFoundException;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import com.pet.walkthroughserver.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityEntryRepository activityEntryRepository;
    private final UserRepository userRepository;

    @Override
    public SliceData<ActivityEntryEntity> getActivity(String username, UUID viewerId, Instant before, int limit) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        boolean isSelf = viewerId != null && viewerId.equals(user.getId());
        PageRequest pageable = PageRequest.of(0, limit + 1);

        List<ActivityEntryEntity> entries;
        if (isSelf) {
            entries = activityEntryRepository.findByUserIdBeforeTime(user.getId(), before, pageable);
        } else {
            entries = activityEntryRepository.findPublicByUserIdBeforeTime(user.getId(), before, pageable);
        }

        boolean hasNext = entries.size() > limit;
        List<ActivityEntryEntity> resultEntries = hasNext ? entries.subList(0, limit) : entries;

        return SliceData.of(resultEntries, hasNext);
    }
}
