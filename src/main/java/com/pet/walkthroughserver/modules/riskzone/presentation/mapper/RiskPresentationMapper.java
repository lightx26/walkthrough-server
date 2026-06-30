package com.pet.walkthroughserver.modules.riskzone.presentation.mapper;

import com.pet.walkthroughserver.modules.riskzone.presentation.dto.*;
import com.pet.walkthroughserver.modules.riskzone.repository.RiskScanEntity;
import com.pet.walkthroughserver.modules.riskzone.repository.RiskZoneEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RiskPresentationMapper {

    private final WalkthroughFileRepository walkthroughFileRepository;

    public RiskScanResponse toScanResponse(RiskScanEntity scan, List<RiskZoneEntity> zones) {
        // batch-load filenames for all file IDs referenced by the zones
        List<UUID> fileIds = zones.stream()
                .map(RiskZoneEntity::getWalkthroughFileId)
                .distinct()
                .toList();
        Map<UUID, String> filenameById = walkthroughFileRepository.findAllById(fileIds).stream()
                .collect(Collectors.toMap(f -> f.getId(), f -> f.getFilename()));

        List<RiskZoneResponse> riskResponses = zones.stream()
                .map(z -> toZoneResponse(z, filenameById.getOrDefault(z.getWalkthroughFileId(), "")))
                .toList();

        List<RiskFileProgressResponse> progressResponses = scan.getFileProgress() == null
                ? List.of()
                : scan.getFileProgress().stream()
                        .map(e -> new RiskFileProgressResponse(e.filename(), e.status(), e.reason()))
                        .toList();

        return new RiskScanResponse(
                scan.getId(),
                scan.getStatus().name(),
                scan.getProvider(),
                scan.getModel(),
                scan.getTotalFiles(),
                scan.getAnalyzedFiles(),
                new RiskCountsResponse(
                        scan.getCriticalCount(),
                        scan.getHighCount(),
                        scan.getMediumCount(),
                        scan.getLowCount()
                ),
                progressResponses,
                riskResponses
        );
    }

    public RiskZoneResponse toZoneResponse(RiskZoneEntity zone, String filename) {
        return new RiskZoneResponse(
                zone.getId(),
                zone.getRiskLevel().name(),
                zone.getCategory().name(),
                zone.getCategory().getLabel(),
                zone.getTitle(),
                zone.getDescription(),
                zone.getSuggestion(),
                filename,
                zone.getWalkthroughFileId(),
                zone.getStartPosition(),
                zone.getEndPosition(),
                zone.getLineSide(),
                zone.getReviewStatus().name()
        );
    }

    public RiskZoneResponse toZoneResponseLookup(RiskZoneEntity zone) {
        String filename = walkthroughFileRepository.findById(zone.getWalkthroughFileId())
                .map(f -> f.getFilename()).orElse("");
        return toZoneResponse(zone, filename);
    }
}
