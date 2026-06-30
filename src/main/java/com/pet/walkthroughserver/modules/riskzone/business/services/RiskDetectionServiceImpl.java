package com.pet.walkthroughserver.modules.riskzone.business.services;

import com.pet.walkthroughserver.modules._shared.infra.ai.AiProperties;
import com.pet.walkthroughserver.modules._shared.infra.ai.LlmClient;
import com.pet.walkthroughserver.modules._shared.infra.ai.LlmResponse;
import com.pet.walkthroughserver.modules._shared.util.UnifiedDiff;
import com.pet.walkthroughserver.modules._shared.util.UnifiedDiff.DiffWindow;
import com.pet.walkthroughserver.modules.riskzone.business.models.ChapterContext;
import com.pet.walkthroughserver.modules.riskzone.business.models.CrossFileRisk;
import com.pet.walkthroughserver.modules.riskzone.business.models.DetectedRisk;
import com.pet.walkthroughserver.modules.riskzone.business.models.FileChangeSummary;
import com.pet.walkthroughserver.modules.riskzone.business.models.FileScanResult;
import com.pet.walkthroughserver.modules.riskzone.business.prompt.RiskPromptBuilder;
import com.pet.walkthroughserver.modules.riskzone.business.prompt.RiskPromptBuilder.ChapterFileDigest;
import com.pet.walkthroughserver.modules.riskzone.business.prompt.RiskResponseParser;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskDetectionServiceImpl implements RiskDetectionService {

    private final LlmClient llmClient;
    private final AiProperties aiProperties;
    private final RiskPromptBuilder promptBuilder;
    private final RiskResponseParser responseParser;

    @Override
    public FileScanResult detect(WalkthroughFileEntity file, ChapterContext context) {
        String rawPatch = file.getRawPatch();
        if (rawPatch == null || rawPatch.isBlank()) {
            return new FileScanResult(List.of(), FileChangeSummary.empty());
        }

        int contextLines = aiProperties.getScan().getWindowContextLines();
        int maxChars = aiProperties.getScan().getMaxWindowChars();
        int maxContextChars = aiProperties.getScan().getMaxContextChars();
        int maxPatchChars = aiProperties.getScan().getMaxPatchChars();

        // File-level guard: trim the tail of an over-large patch before window extraction so a file
        // with many windows can't blow out the prompt. Tail-trimming is position-safe — diff
        // positions count from the first hunk header, so positions still validate against rawPatch.
        String patchForWindows = rawPatch.length() > maxPatchChars
                ? rawPatch.substring(0, maxPatchChars)
                : rawPatch;

        List<DiffWindow> windows = UnifiedDiff.extractChangedWindows(patchForWindows, contextLines);
        if (windows.isEmpty()) {
            return new FileScanResult(List.of(), FileChangeSummary.empty());
        }

        // truncate oversized windows
        windows = windows.stream()
                .map(w -> w.text().length() > maxChars
                        ? new DiffWindow(w.startPosition(), w.endPosition(), w.side(),
                                         w.text().substring(0, maxChars) + "\n[truncated]")
                        : w)
                .toList();

        var request = promptBuilder.build(file.getFilename(), file.getFileStatus(), windows,
                context, maxContextChars);
        LlmResponse response = llmClient.complete(request);

        FileScanResult result = responseParser.parseFileScan(response.content());
        List<DetectedRisk> validated = validatePositions(rawPatch, result.risks());

        log.info("Detected {} risks in {} (provider={})", validated.size(), file.getFilename(),
                llmClient.providerName());
        return new FileScanResult(validated, result.changeSummary());
    }

    @Override
    public List<CrossFileRisk> detectCrossFile(ChapterContext context,
                                               List<WalkthroughFileEntity> files,
                                               Map<UUID, FileScanResult> resultsByFile) {
        if (files == null || files.size() < 2) return List.of();

        int maxContextChars = aiProperties.getScan().getMaxContextChars();

        List<ChapterFileDigest> digests = new ArrayList<>();
        for (WalkthroughFileEntity f : files) {
            FileScanResult result = resultsByFile.getOrDefault(f.getId(),
                    new FileScanResult(List.of(), FileChangeSummary.empty()));
            List<String> titles = result.risks().stream()
                    .map(DetectedRisk::title)
                    .toList();
            digests.add(new ChapterFileDigest(
                    f.getFilename(),
                    f.getFileStatus(),
                    result.changeSummary(),
                    titles));
        }

        var request = promptBuilder.buildChapterReduce(context, digests, maxContextChars);
        LlmResponse response = llmClient.complete(request);

        List<CrossFileRisk> risks = responseParser.parseCrossFile(response.content());
        log.info("Detected {} cross-file risks in chapter '{}' ({} files, provider={})",
                risks.size(), context.chapterTitle(), files.size(), llmClient.providerName());
        return risks;
    }

    /** Null out any diff positions the model invented so they don't get persisted as overlays. */
    private List<DetectedRisk> validatePositions(String rawPatch, List<DetectedRisk> risks) {
        return risks.stream()
                .map(r -> {
                    Integer start = r.startPosition() != null && UnifiedDiff.isValidPosition(rawPatch, r.startPosition())
                            ? r.startPosition() : null;
                    Integer end = r.endPosition() != null && UnifiedDiff.isValidPosition(rawPatch, r.endPosition())
                            ? r.endPosition() : null;
                    String side = (start != null) ? r.lineSide() : null;
                    return new DetectedRisk(r.riskLevel(), r.category(), r.title(),
                            r.description(), r.suggestion(), start, end, side);
                })
                .toList();
    }
}
