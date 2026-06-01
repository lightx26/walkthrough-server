package com.pet.walkthroughserver.modules.profile.presentation.assembler;

import java.util.List;

import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.modules._shared.dto.SliceData;
import com.pet.walkthroughserver.modules.profile.business.models.PinnedWalkthrough;
import com.pet.walkthroughserver.modules.profile.business.models.ProfileData;
import com.pet.walkthroughserver.modules.profile.business.models.ProfileStats;
import com.pet.walkthroughserver.modules.profile.business.models.ReviewingEntry;
import com.pet.walkthroughserver.modules.profile.presentation.dto.ActivityEntryResponse;
import com.pet.walkthroughserver.modules.profile.presentation.dto.PinnedWalkthroughResponse;
import com.pet.walkthroughserver.modules.profile.presentation.dto.ProfileResponse;
import com.pet.walkthroughserver.modules.profile.presentation.dto.ProfileReviewingResponse;
import com.pet.walkthroughserver.modules.profile.presentation.dto.ProfileStatsResponse;
import com.pet.walkthroughserver.modules.profile.repository.ActivityEntryEntity;

@Component
public class ProfileAssembler {

    public ProfileResponse toResponse(ProfileData model, boolean includeSensitive) {
        ProfileResponse.ProfileResponseBuilder builder = ProfileResponse.builder()
                .id(model.id().toString())
                .username(model.username())
                .displayName(model.displayName())
                .avatarUrl(model.avatarUrl())
                .githubUrl(model.githubUrl())
                .joinedAt(model.joinedAt());

        if (includeSensitive) {
            builder.email(model.email());
        }

        return builder.build();
    }

    public ProfileStatsResponse toResponse(ProfileStats model) {
        return ProfileStatsResponse.builder()
                .walkthroughs(model.walkthroughs())
                .chapters(model.chapters())
                .views(model.views())
                .comments(model.comments())
                .pins(model.pins())
                .reviews(model.reviews())
                .build();
    }

    public ProfileReviewingResponse toResponse(ReviewingEntry model) {
        return ProfileReviewingResponse.builder()
                .walkthroughId(model.walkthroughId())
                .title(model.title())
                .owner(model.owner())
                .repo(model.repo())
                .prNumber(model.prNumber())
                .status(model.status())
                .creatorDisplayName(model.creatorDisplayName())
                .creatorAvatarUrl(model.creatorAvatarUrl())
                .readChapters(model.readChapters())
                .totalChapters(model.totalChapters())
                .timeSpentSec(model.timeSpentSec())
                .lastReadAt(model.lastReadAt())
                .build();
    }

    public List<ProfileReviewingResponse> toReviewingResponseList(List<ReviewingEntry> models) {
        return models.stream().map(this::toResponse).toList();
    }

    public PinnedWalkthroughResponse toResponse(PinnedWalkthrough model) {
        return PinnedWalkthroughResponse.builder()
                .id(model.pinId().toString())
                .walkthroughId(model.walkthroughId().toString())
                .title(model.title())
                .owner(model.owner())
                .repo(model.repo())
                .prNumber(model.prNumber())
                .status(model.status().name())
                .sortOrder(model.sortOrder())
                .pinnedAt(model.pinnedAt())
                .build();
    }

    public List<PinnedWalkthroughResponse> toPinnedResponseList(List<PinnedWalkthrough> models) {
        return models.stream().map(this::toResponse).toList();
    }

    public ActivityEntryResponse toResponse(ActivityEntryEntity entity) {
        return ActivityEntryResponse.builder()
                .id(entity.getId().toString())
                .eventType(entity.getEventType())
                .occurredAt(entity.getOccurredAt())
                .walkthroughId(entity.getWalkthroughId() != null ? entity.getWalkthroughId().toString() : null)
                .chapterId(entity.getChapterId() != null ? entity.getChapterId().toString() : null)
                .commentId(entity.getCommentId() != null ? entity.getCommentId().toString() : null)
                .visibility(entity.getVisibility())
                .metadata(entity.getMetadata())
                .build();
    }

    public SliceData<ActivityEntryResponse> toActivitySlice(SliceData<ActivityEntryEntity> slice) {
        List<ActivityEntryResponse> responses = slice.getItems().stream()
                .map(this::toResponse)
                .toList();
        return SliceData.of(responses, slice.isHasNext());
    }
}
