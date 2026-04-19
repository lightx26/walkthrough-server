package com.pet.walkthroughserver.modules.profile.business.services;

import com.pet.walkthroughserver.modules.profile.presentation.dto.ProfileResponse;
import com.pet.walkthroughserver.modules.profile.presentation.dto.ProfileStatsResponse;
import com.pet.walkthroughserver.modules.profile.repository.WalkthroughPinRepository;
import com.pet.walkthroughserver.modules.user.exceptions.UserNotFoundException;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import com.pet.walkthroughserver.modules.user.repository.UserRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final WalkthroughRepository walkthroughRepository;
    private final WalkthroughPinRepository pinRepository;

    @Override
    public ProfileResponse getMyProfile(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return toProfileResponse(user, true);
    }

    @Override
    public ProfileResponse getByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return toProfileResponse(user, false);
    }

    @Override
    public ProfileStatsResponse getStats(String username, UUID viewerId) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        boolean isSelf = viewerId != null && viewerId.equals(user.getId());

        long walkthroughCount;
        if (isSelf) {
            walkthroughCount = walkthroughRepository.countByUserId(user.getId());
        } else {
            walkthroughCount = walkthroughRepository.countByUserIdAndStatus(user.getId(), WalkthroughStatus.PUBLISHED);
        }

        long chapterCount = walkthroughRepository.countChaptersByUserId(user.getId());
        long viewCount = walkthroughRepository.countViewsByUserId(user.getId());
        long commentCount = walkthroughRepository.countCommentsByUserId(user.getId());
        long pinCount = pinRepository.countByUserId(user.getId());

        return ProfileStatsResponse.builder()
                .walkthroughs(walkthroughCount)
                .chapters(chapterCount)
                .views(viewCount)
                .comments(commentCount)
                .pins(pinCount)
                .build();
    }

    private ProfileResponse toProfileResponse(UserEntity user, boolean includeSensitive) {
        ProfileResponse.ProfileResponseBuilder builder = ProfileResponse.builder()
                .id(user.getId().toString())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .githubUrl("https://github.com/" + user.getUsername())
                .joinedAt(user.getCreatedAt());

        if (includeSensitive) {
            builder.email(user.getEmail());
        }

        return builder.build();
    }
}
