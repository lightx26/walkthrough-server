package com.pet.walkthroughserver.modules.profile.business.services;

import com.pet.walkthroughserver.configs.CacheNames;
import com.pet.walkthroughserver.modules.profile.presentation.dto.ProfileResponse;
import com.pet.walkthroughserver.modules.profile.presentation.dto.ProfileReviewingResponse;
import com.pet.walkthroughserver.modules.profile.presentation.dto.ProfileStatsResponse;
import com.pet.walkthroughserver.modules.profile.repository.WalkthroughPinRepository;
import com.pet.walkthroughserver.modules.user.exceptions.UserNotFoundException;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import com.pet.walkthroughserver.modules.user.repository.UserRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.ReadProgressEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ReadProgressRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final WalkthroughRepository walkthroughRepository;
    private final WalkthroughPinRepository pinRepository;
    private final ReadProgressRepository readProgressRepository;

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
    @Cacheable(value = CacheNames.PROFILE_STATS, key = "#username")
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
        long reviewCount = readProgressRepository.countByUserId(user.getId());

        return ProfileStatsResponse.builder()
                .walkthroughs(walkthroughCount)
                .chapters(chapterCount)
                .views(viewCount)
                .comments(commentCount)
                .pins(pinCount)
                .reviews(reviewCount)
                .build();
    }

    @Override
    public List<ProfileReviewingResponse> getReviewing(String username, UUID viewerId) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        boolean isSelf = viewerId != null && viewerId.equals(user.getId());
        if (!isSelf) {
            return Collections.emptyList();
        }

        List<ReadProgressEntity> progressList = readProgressRepository.findRecentlyReviewed(user.getId());

        return progressList.stream().map(rp -> {
            WalkthroughEntity walkthrough = walkthroughRepository.findById(rp.getWalkthroughId())
                    .orElseThrow();
            UserEntity creator = userRepository.findById(walkthrough.getUserId())
                    .orElseThrow();
            return ProfileReviewingResponse.builder()
                    .walkthroughId(rp.getWalkthroughId())
                    .title(walkthrough.getTitle())
                    .owner(walkthrough.getOwner())
                    .repo(walkthrough.getRepo())
                    .prNumber(walkthrough.getPrNumber())
                    .status(walkthrough.getStatus())
                    .creatorDisplayName(creator.getDisplayName())
                    .creatorAvatarUrl(creator.getAvatarUrl())
                    .readChapters(rp.getReadChapters())
                    .totalChapters(rp.getTotalChapters())
                    .timeSpentSec(rp.getTimeSpentSec())
                    .lastReadAt(rp.getReadAt())
                    .build();
        }).toList();
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
