package com.pet.walkthroughserver.modules.review.presentation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pet.walkthroughserver.interceptors.DataResponse;
import com.pet.walkthroughserver.modules._shared.dto.ListData;
import com.pet.walkthroughserver.modules.review.business.services.ReviewDecisionService;
import com.pet.walkthroughserver.modules.review.exceptions.InvalidReviewDecisionException;
import com.pet.walkthroughserver.modules.review.presentation.dto.ReviewDecisionResponse;
import com.pet.walkthroughserver.modules.review.presentation.dto.SubmitReviewDecisionRequest;
import com.pet.walkthroughserver.modules.review.presentation.mapper.ReviewDecisionPresentationMapper;
import com.pet.walkthroughserver.modules.review.repository.ReviewDecision;
import com.pet.walkthroughserver.modules.review.repository.ReviewDecisionEntity;
import com.pet.walkthroughserver.security.AuthUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/walkthroughs")
@RequiredArgsConstructor
public class ReviewDecisionController {

    private final ReviewDecisionService reviewDecisionService;
    private final ReviewDecisionPresentationMapper reviewDecisionMapper;

    @PostMapping("/{walkthroughId}/review-decision")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ReviewDecisionResponse>> submitDecision(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID walkthroughId,
            @Valid @RequestBody SubmitReviewDecisionRequest request) {
        UUID userId = UUID.fromString(authUser.getUserId());
        ReviewDecision decision = parseDecision(request.getDecision());
        ReviewDecisionEntity entity = reviewDecisionService.upsertDecision(userId, walkthroughId, decision, request.getComment());
        return ResponseEntity.status(HttpStatus.CREATED).body(DataResponse.of(reviewDecisionMapper.toResponse(entity)));
    }

    @GetMapping("/{walkthroughId}/review-decisions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<ReviewDecisionResponse>>> listDecisions(
            @PathVariable UUID walkthroughId) {
        List<ReviewDecisionEntity> entities = reviewDecisionService.listDecisions(walkthroughId);
        List<ReviewDecisionResponse> responses = entities.stream()
                .map(reviewDecisionMapper::toResponse)
                .toList();
        return ResponseEntity.ok(DataResponse.of(ListData.of(responses)));
    }

    @GetMapping("/{walkthroughId}/review-decision/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ReviewDecisionResponse>> getMyDecision(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID walkthroughId) {
        UUID userId = UUID.fromString(authUser.getUserId());
        Optional<ReviewDecisionEntity> entity = reviewDecisionService.getMyDecision(userId, walkthroughId);
        return entity.map(e -> ResponseEntity.ok(DataResponse.of(reviewDecisionMapper.toResponse(e))))
                .orElse(ResponseEntity.noContent().build());
    }

    @DeleteMapping("/{walkthroughId}/review-decision")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> withdrawDecision(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID walkthroughId) {
        UUID userId = UUID.fromString(authUser.getUserId());
        reviewDecisionService.withdrawDecision(userId, walkthroughId);
        return ResponseEntity.noContent().build();
    }

    private ReviewDecision parseDecision(String value) {
        try {
            return ReviewDecision.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidReviewDecisionException("Decision must be APPROVED or REJECTED");
        }
    }
}
