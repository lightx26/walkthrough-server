package com.pet.walkthroughserver.modules.riskzone.presentation;

import com.pet.walkthroughserver.interceptors.DataResponse;
import com.pet.walkthroughserver.modules.riskzone.business.services.RiskReviewService;
import com.pet.walkthroughserver.modules.riskzone.business.services.RiskScanService;
import com.pet.walkthroughserver.modules.riskzone.presentation.dto.MarkReviewedRequest;
import com.pet.walkthroughserver.modules.riskzone.presentation.dto.RiskScanResponse;
import com.pet.walkthroughserver.modules.riskzone.presentation.dto.RiskZoneResponse;
import com.pet.walkthroughserver.modules.riskzone.presentation.mapper.RiskPresentationMapper;
import com.pet.walkthroughserver.modules.riskzone.repository.RiskScanEntity;
import com.pet.walkthroughserver.modules.riskzone.repository.RiskZoneEntity;
import com.pet.walkthroughserver.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/walkthroughs/{walkthroughId}/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskScanService riskScanService;
    private final RiskReviewService riskReviewService;
    private final RiskPresentationMapper mapper;

    @PostMapping("/scan")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<RiskScanResponse>> triggerScan(
            @PathVariable UUID walkthroughId,
            @AuthenticationPrincipal AuthUser authUser) {
        UUID userId = UUID.fromString(authUser.getUserId());
        RiskScanEntity scan = riskScanService.requestScan(walkthroughId, userId);
        RiskScanResponse response = mapper.toScanResponse(scan, List.of());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(DataResponse.of(response));
    }

    @GetMapping("/scan")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<RiskScanResponse>> getLatestScan(
            @PathVariable UUID walkthroughId) {
        RiskScanEntity scan = riskScanService.getLatestScan(walkthroughId);
        List<RiskZoneEntity> zones = riskScanService.getRisksForScan(scan.getId());
        RiskScanResponse response = mapper.toScanResponse(scan, zones);
        return ResponseEntity.ok(DataResponse.of(response));
    }

    @PostMapping("/zones/{riskId}/review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<RiskZoneResponse>> markReviewed(
            @PathVariable UUID walkthroughId,
            @PathVariable UUID riskId,
            @RequestBody MarkReviewedRequest request) {
        RiskZoneEntity zone = riskReviewService.markReviewed(riskId, request.reviewed());
        RiskZoneResponse response = mapper.toZoneResponseLookup(zone);
        return ResponseEntity.ok(DataResponse.of(response));
    }
}
