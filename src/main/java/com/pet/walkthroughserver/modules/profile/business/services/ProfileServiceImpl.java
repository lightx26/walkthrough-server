package com.pet.walkthroughserver.modules.profile.business.services;

import com.pet.walkthroughserver.configs.CacheNames;
import com.pet.walkthroughserver.modules.profile.business.models.ProfileData;
import com.pet.walkthroughserver.modules.profile.business.models.ProfileStats;
import com.pet.walkthroughserver.modules.profile.business.models.ReviewingEntry;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final WalkthroughRepository walkthroughRepository;
    private final WalkthroughPinRepository pinRepository;
    private final ReadProgressRepository readProgressRepository;

    @Override
    public ProfileData getMyProfile(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return toProfileData(user);
    }

    @Override
    public ProfileData getByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return toProfileData(user);
    }

    @Override
    @Cacheable(value = CacheNames.PROFILE_STATS, key = "#username")
    public ProfileStats getStats(String username, UUID viewerId) {
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

        return new ProfileStats(walkthroughCount, chapterCount, viewCount, commentCount, pinCount, reviewCount);
    }

    @Override
    public List<ReviewingEntry> getReviewing(String username, UUID viewerId) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        boolean isSelf = viewerId != null && viewerId.equals(user.getId());
        if (!isSelf) {
            return Collections.emptyList();
        }

        List<ReadProgressEntity> progressList = readProgressRepository.findRecentlyReviewed(user.getId());

        // Batch-load walkthroughs and creators to fix the N+2 query issue
        Set<UUID> walkthroughIds = progressList.stream()
                .map(ReadProgressEntity::getWalkthroughId)
                .collect(Collectors.toSet());
        Map<UUID, WalkthroughEntity> walkthroughMap = walkthroughRepository.findAllById(walkthroughIds)
                .stream().collect(Collectors.toMap(WalkthroughEntity::getId, Function.identity()));

        Set<UUID> creatorIds = walkthroughMap.values().stream()
                .map(WalkthroughEntity::getUserId)
                .collect(Collectors.toSet());
        Map<UUID, UserEntity> creatorMap = userRepository.findAllById(creatorIds)
                .stream().collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        return progressList.stream()
                .filter(rp -> walkthroughMap.containsKey(rp.getWalkthroughId()))
                .map(rp -> {
                    WalkthroughEntity walkthrough = walkthroughMap.get(rp.getWalkthroughId());
                    UserEntity creator = creatorMap.get(walkthrough.getUserId());
                    return new ReviewingEntry(
                            rp.getWalkthroughId(),
                            walkthrough.getTitle(),
                            walkthrough.getOwner(),
                            walkthrough.getRepo(),
                            walkthrough.getPrNumber(),
                            walkthrough.getStatus(),
                            creator != null ? creator.getDisplayName() : null,
                            creator != null ? creator.getAvatarUrl() : null,
                            rp.getReadChapters(),
                            rp.getTotalChapters(),
                            rp.getTimeSpentSec(),
                            rp.getReadAt());
                }).toList();
    }

    private ProfileData toProfileData(UserEntity user) {
        return new ProfileData(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getAvatarUrl(),
                null,
                "https://github.com/" + user.getUsername(),
                user.getCreatedAt());
    }
}
