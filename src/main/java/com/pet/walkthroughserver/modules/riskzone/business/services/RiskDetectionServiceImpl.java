package com.pet.walkthroughserver.modules.riskzone.business.services;

import com.pet.walkthroughserver.modules._shared.infra.ai.AiProperties;
import com.pet.walkthroughserver.modules._shared.infra.ai.LlmClient;
import com.pet.walkthroughserver.modules._shared.infra.ai.LlmResponse;
import com.pet.walkthroughserver.modules._shared.util.UnifiedDiff;
import com.pet.walkthroughserver.modules._shared.util.UnifiedDiff.DiffWindow;
import com.pet.walkthroughserver.modules.riskzone.business.models.ChapterContext;
import com.pet.walkthroughserver.modules.riskzone.business.models.CrossFileRisk;
import com.pet.walkthroughserver.modules.riskzone.business.models.DetectedRisk;
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

    /** Cap on hunk headers per file fed to the reduce pass — keeps the prompt bounded. */
    private static final int MAX_HUNK_HEADERS_PER_FILE = 40;

    private final LlmClient llmClient;
    private final AiProperties aiProperties;
    private final RiskPromptBuilder promptBuilder;
    private final RiskResponseParser responseParser;

    @Override
    public List<DetectedRisk> detect(WalkthroughFileEntity file, ChapterContext context) {
        String rawPatch = file.getRawPatch();
        if (rawPatch == null || rawPatch.isBlank()) return List.of();

        int contextLines = aiProperties.getScan().getWindowContextLines();
        int maxChars = aiProperties.getScan().getMaxWindowChars();
        int maxContextChars = aiProperties.getScan().getMaxContextChars();

        List<DiffWindow> windows = UnifiedDiff.extractChangedWindows(rawPatch, contextLines);
        if (windows.isEmpty()) return List.of();

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

        List<DetectedRisk> risks = responseParser.parse(response.content());
        risks = validatePositions(rawPatch, risks);

        log.info("Detected {} risks in {} (provider={})", risks.size(), file.getFilename(), llmClient.providerName());
        return risks;
    }

    @Override
    public List<CrossFileRisk> detectCrossFile(ChapterContext context,
                                               List<WalkthroughFileEntity> files,
                                               Map<UUID, List<DetectedRisk>> risksByFile) {
        if (files == null || files.size() < 2) return List.of();

        List<ChapterFileDigest> digests = new ArrayList<>();
        for (WalkthroughFileEntity f : files) {
            List<String> titles = risksByFile.getOrDefault(f.getId(), List.of()).stream()
                    .map(DetectedRisk::title)
                    .toList();
            digests.add(new ChapterFileDigest(
                    f.getFilename(),
                    f.getFileStatus(),
                    extractHunkHeaders(f.getRawPatch()),
                    titles));
        }

        int maxContextChars = aiProperties.getScan().getMaxContextChars();
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

    /** Collect the {@code @@ … @@} hunk headers of a patch, bounded for prompt size. */
    private String extractHunkHeaders(String rawPatch) {
        if (rawPatch == null || rawPatch.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String line : rawPatch.split("\n")) {
            if (line.startsWith("@@")) {
                if (count++ >= MAX_HUNK_HEADERS_PER_FILE) {
                    sb.append("  …(more)\n");
                    break;
                }
                sb.append("  ").append(line.trim()).append('\n');
            }
        }
        return sb.toString();
    }
}
