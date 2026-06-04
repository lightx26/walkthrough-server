package com.pet.walkthroughserver.modules.riskzone.business.services;

import com.pet.walkthroughserver.modules._shared.infra.ai.AiProperties;
import com.pet.walkthroughserver.modules._shared.infra.ai.LlmClient;
import com.pet.walkthroughserver.modules._shared.infra.ai.LlmResponse;
import com.pet.walkthroughserver.modules._shared.util.UnifiedDiff;
import com.pet.walkthroughserver.modules._shared.util.UnifiedDiff.DiffWindow;
import com.pet.walkthroughserver.modules.riskzone.business.models.DetectedRisk;
import com.pet.walkthroughserver.modules.riskzone.business.prompt.RiskPromptBuilder;
import com.pet.walkthroughserver.modules.riskzone.business.prompt.RiskResponseParser;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskDetectionServiceImpl implements RiskDetectionService {

    private final LlmClient llmClient;
    private final AiProperties aiProperties;
    private final RiskPromptBuilder promptBuilder;
    private final RiskResponseParser responseParser;

    @Override
    public List<DetectedRisk> detect(WalkthroughFileEntity file) {
        String rawPatch = file.getRawPatch();
        if (rawPatch == null || rawPatch.isBlank()) return List.of();

        int contextLines = aiProperties.getScan().getWindowContextLines();
        int maxChars = aiProperties.getScan().getMaxWindowChars();

        List<DiffWindow> windows = UnifiedDiff.extractChangedWindows(rawPatch, contextLines);
        if (windows.isEmpty()) return List.of();

        // truncate oversized windows
        windows = windows.stream()
                .map(w -> w.text().length() > maxChars
                        ? new DiffWindow(w.startPosition(), w.endPosition(), w.side(),
                                         w.text().substring(0, maxChars) + "\n[truncated]")
                        : w)
                .toList();

        var request = promptBuilder.build(file.getFilename(), file.getFileStatus(), windows);
        LlmResponse response = llmClient.complete(request);

        List<DetectedRisk> risks = responseParser.parse(response.content());

        // validate positions against the actual patch; null out any that don't exist
        risks = risks.stream()
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

        log.info("Detected {} risks in {} (provider={})", risks.size(), file.getFilename(), llmClient.providerName());
        return risks;
    }
}
