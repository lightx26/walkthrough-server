package com.pet.walkthroughserver.modules.riskzone.business.sync;

import com.pet.walkthroughserver.modules._shared.infra.ai.AiProperties;
import com.pet.walkthroughserver.modules._shared.infra.ai.LlmClient;
import com.pet.walkthroughserver.modules.riskzone.business.models.DetectedRisk;
import com.pet.walkthroughserver.modules.riskzone.business.services.RiskDetectionService;
import com.pet.walkthroughserver.modules.riskzone.repository.*;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskScanHandlerImpl implements RiskScanHandler {

    private final RiskScanRepository riskScanRepository;
    private final RiskZoneRepository riskZoneRepository;
    private final WalkthroughRepository walkthroughRepository;
    private final RiskDetectionService riskDetectionService;
    private final LlmClient llmClient;
    private final AiProperties aiProperties;

    @Override
    @Transactional
    public void handle(RiskScanCommand command) {
        RiskScanEntity scan = riskScanRepository.findById(command.scanId())
                .orElseThrow(() -> new IllegalStateException("Risk scan not found: " + command.scanId()));

        WalkthroughEntity walkthrough = walkthroughRepository.findById(scan.getWalkthroughId())
                .orElseThrow(() -> new IllegalStateException("Walkthrough not found: " + scan.getWalkthroughId()));

        List<WalkthroughFileEntity> allFiles = walkthrough.getChapters().stream()
                .flatMap(ch -> ch.getFiles().stream())
                .filter(f -> f.getRawPatch() != null && !f.getRawPatch().isBlank())
                .limit(aiProperties.getScan().getMaxFiles())
                .toList();

        // initialize scan
        List<FileProgressEntry> progress = new ArrayList<>();
        for (WalkthroughFileEntity f : allFiles) {
            progress.add(new FileProgressEntry(f.getFilename(), "pending"));
        }
        scan.setStatus(RiskScanStatus.ANALYZING);
        scan.setProvider(llmClient.providerName());
        scan.setModel(aiProperties.getDeepseek().getModel());
        scan.setTotalFiles(allFiles.size());
        scan.setAnalyzedFiles(0);
        scan.setFileProgress(progress);
        riskScanRepository.save(scan);

        try {
            for (int i = 0; i < allFiles.size(); i++) {
                WalkthroughFileEntity file = allFiles.get(i);
                updateFileStatus(scan, i, "analyzing");
                riskScanRepository.save(scan);

                try {
                    List<DetectedRisk> risks = riskDetectionService.detect(file);
                    persistRisks(scan, file.getId(), risks);
                    updateCounts(scan, risks);
                    scan.setAnalyzedFiles(scan.getAnalyzedFiles() + 1);
                    updateFileStatus(scan, i, "done");
                } catch (Exception e) {
                    log.error("Risk detection failed for file {}: {}", file.getFilename(), e.getMessage(), e);
                    updateFileStatus(scan, i, "failed");
                }

                riskScanRepository.save(scan);
            }

            scan.setStatus(RiskScanStatus.COMPLETED);
            riskScanRepository.save(scan);
            log.info("Risk scan {} completed: {} files, critical={}, high={}, medium={}, low={}",
                    scan.getId(), allFiles.size(),
                    scan.getCriticalCount(), scan.getHighCount(),
                    scan.getMediumCount(), scan.getLowCount());

        } catch (Exception e) {
            log.error("Risk scan {} failed: {}", scan.getId(), e.getMessage(), e);
            scan.setStatus(RiskScanStatus.FAILED);
            scan.setErrorMessage(e.getMessage());
            riskScanRepository.save(scan);
            throw e;
        }
    }

    private void updateFileStatus(RiskScanEntity scan, int index, String status) {
        List<FileProgressEntry> progress = new ArrayList<>(scan.getFileProgress());
        if (index < progress.size()) {
            FileProgressEntry old = progress.get(index);
            progress.set(index, new FileProgressEntry(old.filename(), status));
            scan.setFileProgress(progress);
        }
    }

    private void persistRisks(RiskScanEntity scan, UUID fileId, List<DetectedRisk> risks) {
        for (DetectedRisk r : risks) {
            RiskZoneEntity zone = RiskZoneEntity.builder()
                    .riskScan(scan)
                    .walkthroughFileId(fileId)
                    .riskLevel(r.riskLevel())
                    .category(r.category())
                    .title(r.title())
                    .description(r.description())
                    .suggestion(r.suggestion())
                    .startPosition(r.startPosition())
                    .endPosition(r.endPosition())
                    .lineSide(r.lineSide())
                    .reviewStatus(ReviewStatus.OPEN)
                    .build();
            riskZoneRepository.save(zone);
        }
    }

    private void updateCounts(RiskScanEntity scan, List<DetectedRisk> risks) {
        for (DetectedRisk r : risks) {
            switch (r.riskLevel()) {
                case CRITICAL -> scan.setCriticalCount(scan.getCriticalCount() + 1);
                case HIGH     -> scan.setHighCount(scan.getHighCount() + 1);
                case MEDIUM   -> scan.setMediumCount(scan.getMediumCount() + 1);
                case LOW      -> scan.setLowCount(scan.getLowCount() + 1);
            }
        }
    }
}
