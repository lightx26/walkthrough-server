package com.pet.walkthroughserver.modules.profile.presentation;

import com.pet.walkthroughserver.interceptors.DataResponse;
import com.pet.walkthroughserver.modules._shared.dto.ListData;
import com.pet.walkthroughserver.modules._shared.dto.SliceData;
import com.pet.walkthroughserver.modules.profile.business.services.ActivityService;
import com.pet.walkthroughserver.modules.profile.business.services.ProfileService;
import com.pet.walkthroughserver.modules.profile.business.services.WalkthroughPinService;
import com.pet.walkthroughserver.modules.profile.presentation.assembler.ProfileAssembler;
import com.pet.walkthroughserver.modules.profile.presentation.dto.ActivityEntryResponse;
import com.pet.walkthroughserver.modules.profile.presentation.dto.PinWalkthroughRequest;
import com.pet.walkthroughserver.modules.profile.presentation.dto.PinnedWalkthroughResponse;
import com.pet.walkthroughserver.modules.profile.presentation.dto.ProfileResponse;
import com.pet.walkthroughserver.modules.profile.presentation.dto.ProfileReviewingResponse;
import com.pet.walkthroughserver.modules.profile.presentation.dto.ProfileStatsResponse;
import com.pet.walkthroughserver.modules.profile.presentation.dto.ReorderPinsRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.WalkthroughSummaryResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.mapper.WalkthroughPresentationMapper;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;
import com.pet.walkthroughserver.modules.user.exceptions.UserNotFoundException;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import com.pet.walkthroughserver.modules.user.repository.UserRepository;
import com.pet.walkthroughserver.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final WalkthroughPinService pinService;
    private final ActivityService activityService;
    private final ProfileAssembler profileAssembler;
    private final WalkthroughRepository walkthroughRepository;
    private final WalkthroughPresentationMapper walkthroughMapper;
    private final UserRepository userRepository;

    // ── Self profile ──

    @GetMapping("/v1/profile/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ProfileResponse>> getMyProfile(
            @AuthenticationPrincipal AuthUser authUser) {
        UUID userId = UUID.fromString(authUser.getUserId());
        ProfileResponse response = profileAssembler.toResponse(
                profileService.getMyProfile(userId), true);
        return ResponseEntity.ok(DataResponse.of(response));
    }

    // ── Public profile ──

    @GetMapping("/v1/users/{username}")
    public ResponseEntity<DataResponse<ProfileResponse>> getByUsername(
            @PathVariable String username) {
        ProfileResponse response = profileAssembler.toResponse(
                profileService.getByUsername(username), false);
        return ResponseEntity.ok(DataResponse.of(response));
    }

    @GetMapping("/v1/users/{username}/stats")
    public ResponseEntity<DataResponse<ProfileStatsResponse>> getStats(
            @PathVariable String username,
            @AuthenticationPrincipal AuthUser authUser) {
        UUID viewerId = authUser != null ? UUID.fromString(authUser.getUserId()) : null;
        ProfileStatsResponse response = profileAssembler.toResponse(
                profileService.getStats(username, viewerId));
        return ResponseEntity.ok(DataResponse.of(response));
    }

    @GetMapping("/v1/users/{username}/walkthroughs")
    public ResponseEntity<DataResponse<ListData<WalkthroughSummaryResponse>>> getUserWalkthroughs(
            @PathVariable String username,
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) WalkthroughStatus status) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        boolean isSelf = authUser != null && UUID.fromString(authUser.getUserId()).equals(user.getId());

        List<WalkthroughEntity> walkthroughs;
        if (status != null && isSelf) {
            walkthroughs = walkthroughRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), status);
        } else if (isSelf) {
            walkthroughs = walkthroughRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
        } else {
            walkthroughs = walkthroughRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(
                    user.getId(), WalkthroughStatus.PUBLISHED);
        }

        List<WalkthroughSummaryResponse> responses = walkthroughMapper.toSummaryResponseList(walkthroughs);
        return ResponseEntity.ok(DataResponse.of(ListData.of(responses)));
    }

    @GetMapping("/v1/users/{username}/reviewing")
    public ResponseEntity<DataResponse<ListData<ProfileReviewingResponse>>> getReviewing(
            @PathVariable String username,
            @AuthenticationPrincipal AuthUser authUser) {
        UUID viewerId = authUser != null ? UUID.fromString(authUser.getUserId()) : null;
        List<ProfileReviewingResponse> result = profileAssembler.toReviewingResponseList(
                profileService.getReviewing(username, viewerId));
        return ResponseEntity.ok(DataResponse.of(ListData.of(result)));
    }

    @GetMapping("/v1/users/{username}/pins")
    public ResponseEntity<DataResponse<ListData<PinnedWalkthroughResponse>>> getPins(
            @PathVariable String username) {
        List<PinnedWalkthroughResponse> pins = profileAssembler.toPinnedResponseList(
                pinService.getPins(username));
        return ResponseEntity.ok(DataResponse.of(ListData.of(pins)));
    }

    @GetMapping("/v1/users/{username}/activity")
    public ResponseEntity<DataResponse<SliceData<ActivityEntryResponse>>> getActivity(
            @PathVariable String username,
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "50") int limit) {
        UUID viewerId = authUser != null ? UUID.fromString(authUser.getUserId()) : null;
        Instant cursor = before != null ? before : Instant.now();
        SliceData<ActivityEntryResponse> result = profileAssembler.toActivitySlice(
                activityService.getActivity(username, viewerId, cursor, limit));
        return ResponseEntity.ok(DataResponse.of(result));
    }

    // ── Pin mutations (self only) ──

    @PostMapping("/v1/profile/me/pins")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<PinnedWalkthroughResponse>> pinWalkthrough(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PinWalkthroughRequest request) {
        UUID userId = UUID.fromString(authUser.getUserId());
        PinnedWalkthroughResponse response = profileAssembler.toResponse(
                pinService.pinWalkthrough(userId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(DataResponse.of(response));
    }

    @PutMapping("/v1/profile/me/pins")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> reorderPins(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ReorderPinsRequest request) {
        UUID userId = UUID.fromString(authUser.getUserId());
        pinService.reorderPins(userId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/v1/profile/me/pins/{pinId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unpinWalkthrough(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID pinId) {
        UUID userId = UUID.fromString(authUser.getUserId());
        pinService.unpinWalkthrough(userId, pinId);
        return ResponseEntity.noContent().build();
    }
}
