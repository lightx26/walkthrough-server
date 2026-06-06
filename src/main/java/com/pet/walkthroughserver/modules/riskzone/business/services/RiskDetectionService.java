package com.pet.walkthroughserver.modules.riskzone.business.services;

import com.pet.walkthroughserver.modules.riskzone.business.models.ChapterContext;
import com.pet.walkthroughserver.modules.riskzone.business.models.CrossFileRisk;
import com.pet.walkthroughserver.modules.riskzone.business.models.DetectedRisk;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface RiskDetectionService {

    /**
     * Per-file (map) detection. The chapter context is injected into the prompt as background
     * so the model understands the change's intent and its sibling files.
     */
    List<DetectedRisk> detect(WalkthroughFileEntity file, ChapterContext context);

    /**
     * Chapter-level (reduce) detection for cross-file/integration risks. Sees a compact digest
     * (hunk headers + already-found risk titles) of every scanned file rather than full diffs.
     *
     * @param context     chapter background
     * @param files       the scanned files of one chapter
     * @param risksByFile per-file risks already found, keyed by walkthrough file id
     * @return cross-file risks, each tagged with the filename it manifests in
     */
    List<CrossFileRisk> detectCrossFile(ChapterContext context,
                                        List<WalkthroughFileEntity> files,
                                        Map<UUID, List<DetectedRisk>> risksByFile);
}
