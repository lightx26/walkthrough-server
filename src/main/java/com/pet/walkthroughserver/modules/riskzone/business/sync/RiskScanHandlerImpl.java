package com.pet.walkthroughserver.modules.riskzone.business.sync;

import com.pet.walkthroughserver.modules._shared.infra.ai.AiProperties;
import com.pet.walkthroughserver.modules._shared.infra.ai.LlmClient;
import com.pet.walkthroughserver.modules._shared.util.UnifiedDiff;
import com.pet.walkthroughserver.modules.riskzone.business.models.ChapterContext;
import com.pet.walkthroughserver.modules.riskzone.business.models.CrossFileRisk;
import com.pet.walkthroughserver.modules.riskzone.business.models.DetectedRisk;
import com.pet.walkthroughserver.modules.riskzone.business.models.FileScanResult;
import com.pet.walkthroughserver.modules.riskzone.business.policy.ScanFileFilter;
import com.pet.walkthroughserver.modules.riskzone.business.policy.ScanFileFilter.Decision;
import com.pet.walkthroughserver.modules.riskzone.business.services.RiskDetectionService;
import com.pet.walkthroughserver.modules.riskzone.repository.*;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskScanHandlerImpl implements RiskScanHandler {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_SKIPPED = "skipped";

    private final RiskScanRepository riskScanRepository;
    private final RiskZoneRepository riskZoneRepository;
    private final WalkthroughRepository walkthroughRepository;
    private final RiskDetectionService riskDetectionService;
    private final ScanFileFilter scanFileFilter;
    private final LlmClient llmClient;
    private final AiProperties aiProperties;

    @Override
    @Transactional
    public void handle(RiskScanCommand command) {
        RiskScanEntity scan = riskScanRepository.findById(command.scanId())
                .orElseThrow(() -> new IllegalStateException("Risk scan not found: " + command.scanId()));

        WalkthroughEntity walkthrough = walkthroughRepository.findById(scan.getWalkthroughId())
                .orElseThrow(() -> new IllegalStateException("Walkthrough not found: " + scan.getWalkthroughId()));

        // Files grouped by chapter, ordered, capped globally at max-files. Preserving chapter
        // grouping lets the reduce phase reason across the files of one chapter; the flattened
        // order matches the file_progress indices used for incremental UI updates. Generated,
        // oversized, and over-cap files are excluded here and reported as "skipped".
        ScanSelection selection = selectFiles(walkthrough, aiProperties.getScan().getMaxFiles());
        Map<ChapterEntity, List<WalkthroughFileEntity>> byChapter = selection.byChapter();
        List<WalkthroughFileEntity> orderedFiles = byChapter.values().stream()
                .flatMap(List::stream)
                .toList();

        // initialize scan: scannable files first (their indices align with orderedFiles for
        // incremental status updates), then the skipped files appended at the tail.
        List<FileProgressEntry> progress = new ArrayList<>();
        for (WalkthroughFileEntity f : orderedFiles) {
            progress.add(new FileProgressEntry(f.getFilename(), STATUS_PENDING));
        }
        progress.addAll(selection.skipped());
        scan.setStatus(RiskScanStatus.ANALYZING);
        scan.setProvider(llmClient.providerName());
        scan.setModel(aiProperties.getDeepseek().getModel());
        scan.setTotalFiles(orderedFiles.size());
        scan.setAnalyzedFiles(0);
        scan.setFileProgress(progress);
        riskScanRepository.save(scan);

        Map<UUID, FileScanResult> resultsByFile = new HashMap<>();

        try {
            // ── Map phase: per-file detection with chapter context ──
            int index = 0;
            for (Map.Entry<ChapterEntity, List<WalkthroughFileEntity>> entry : byChapter.entrySet()) {
                ChapterContext context = ChapterContext.from(entry.getKey());

                for (WalkthroughFileEntity file : entry.getValue()) {
                    updateFileStatus(scan, index, "analyzing");
                    riskScanRepository.save(scan);

                    try {
                        FileScanResult result = riskDetectionService.detect(file, context);
                        persistRisks(scan, file.getId(), result.risks());
                        updateCounts(scan, result.risks());
                        resultsByFile.put(file.getId(), result);
                        scan.setAnalyzedFiles(scan.getAnalyzedFiles() + 1);
                        updateFileStatus(scan, index, "done");
                    } catch (Exception e) {
                        log.error("Risk detection failed for file {}: {}", file.getFilename(), e.getMessage(), e);
                        updateFileStatus(scan, index, "failed");
                    }

                    riskScanRepository.save(scan);
                    index++;
                }
            }

            // ── Reduce phase: cross-file analysis per chapter (best-effort) ──
            if (aiProperties.getScan().isCrossFileAnalysis()) {
                for (Map.Entry<ChapterEntity, List<WalkthroughFileEntity>> entry : byChapter.entrySet()) {
                    runCrossFileAnalysis(scan, entry.getKey(), entry.getValue(), resultsByFile);
                }
            }

            scan.setStatus(RiskScanStatus.COMPLETED);
            riskScanRepository.save(scan);
            log.info("Risk scan {} completed: {} files, critical={}, high={}, medium={}, low={}",
                    scan.getId(), orderedFiles.size(),
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

    /**
     * Cross-file (reduce) analysis for a single chapter. Deliberately best-effort: any failure
     * is logged and swallowed so it can never flip a successful map phase to FAILED. Skipped for
     * chapters below the configured file threshold (cross-file risks need ≥2 files).
     */
    private void runCrossFileAnalysis(RiskScanEntity scan, ChapterEntity chapter,
                                      List<WalkthroughFileEntity> files,
                                      Map<UUID, FileScanResult> resultsByFile) {
        if (files.size() < aiProperties.getScan().getMinFilesForCrossFile()) return;

        try {
            ChapterContext context = ChapterContext.from(chapter);
            List<CrossFileRisk> crossFileRisks =
                    riskDetectionService.detectCrossFile(context, files, resultsByFile);
            if (crossFileRisks.isEmpty()) return;

            // map reported filename → file id, validating positions against that file's patch
            Map<String, WalkthroughFileEntity> byName = new HashMap<>();
            for (WalkthroughFileEntity f : files) byName.put(f.getFilename(), f);

            List<DetectedRisk> persisted = new ArrayList<>();
            for (CrossFileRisk cf : crossFileRisks) {
                WalkthroughFileEntity target = byName.get(cf.filename());
                if (target == null) {
                    log.warn("Dropping cross-file risk: filename '{}' not in chapter '{}'",
                            cf.filename(), chapter.getTitle());
                    continue;
                }
                DetectedRisk risk = validatePositions(target.getRawPatch(), cf.risk());
                persistRisks(scan, target.getId(), List.of(risk));
                persisted.add(risk);
            }

            if (!persisted.isEmpty()) {
                updateCounts(scan, persisted);
                riskScanRepository.save(scan);
                log.info("Cross-file analysis added {} risks to chapter '{}'",
                        persisted.size(), chapter.getTitle());
            }
        } catch (Exception e) {
            log.error("Cross-file analysis failed for chapter '{}': {} (continuing)",
                    chapter.getTitle(), e.getMessage(), e);
        }
    }

    /** The outcome of file selection: scannable files grouped by chapter, plus the skipped files. */
    private record ScanSelection(
            Map<ChapterEntity, List<WalkthroughFileEntity>> byChapter,
            List<FileProgressEntry> skipped
    ) {}

    /**
     * Select scannable files grouped by chapter (insertion-ordered), capped globally. Generated
     * and oversized files (per {@link ScanFileFilter}) and files beyond the global cap are excluded
     * and returned as "skipped" progress entries carrying a user-facing reason. Files with no diff
     * content are dropped silently.
     */
    private ScanSelection selectFiles(WalkthroughEntity walkthrough, int maxFiles) {
        Map<ChapterEntity, List<WalkthroughFileEntity>> byChapter = new LinkedHashMap<>();
        List<FileProgressEntry> skipped = new ArrayList<>();
        int remaining = maxFiles;
        for (ChapterEntity chapter : walkthrough.getChapters()) {
            for (WalkthroughFileEntity file : chapter.getFiles()) {
                Decision decision = scanFileFilter.decide(file);
                if (decision.isVisibleSkip()) {
                    skipped.add(new FileProgressEntry(file.getFilename(), STATUS_SKIPPED, decision.reason()));
                    continue;
                }
                if (!decision.isScan()) continue; // no diff content — drop silently
                if (remaining <= 0) {
                    skipped.add(new FileProgressEntry(file.getFilename(), STATUS_SKIPPED,
                            "Scan limit reached (max " + maxFiles + " files)"));
                    continue;
                }
                byChapter.computeIfAbsent(chapter, c -> new ArrayList<>()).add(file);
                remaining--;
            }
        }
        return new ScanSelection(byChapter, skipped);
    }

    /** Null out invented diff positions so they aren't persisted as bogus inline overlays. */
    private DetectedRisk validatePositions(String rawPatch, DetectedRisk r) {
        Integer start = r.startPosition() != null && UnifiedDiff.isValidPosition(rawPatch, r.startPosition())
                ? r.startPosition() : null;
        Integer end = r.endPosition() != null && UnifiedDiff.isValidPosition(rawPatch, r.endPosition())
                ? r.endPosition() : null;
        String side = (start != null) ? r.lineSide() : null;
        return new DetectedRisk(r.riskLevel(), r.category(), r.title(),
                r.description(), r.suggestion(), start, end, side);
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
