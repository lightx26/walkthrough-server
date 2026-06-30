package com.pet.walkthroughserver.modules.riskzone.business.services;

import com.pet.walkthroughserver.modules.riskzone.business.models.ChapterContext;
import com.pet.walkthroughserver.modules.riskzone.business.models.CrossFileRisk;
import com.pet.walkthroughserver.modules.riskzone.business.models.FileScanResult;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface RiskDetectionService {

    /**
     * Per-file (map) detection. Returns both the detected risks and a semantic summary of
     * what changed in the file's public/exported surface. The summary is forwarded to the
     * cross-file reduce pass so it can reason about contract mismatches across files.
     */
    FileScanResult detect(WalkthroughFileEntity file, ChapterContext context);

    /**
     * Chapter-level (reduce) detection for cross-file/integration risks. Each file's
     * {@link FileScanResult} carries the exported-symbol summary produced by the map pass;
     * no raw diff content is forwarded to keep the prompt bounded.
     *
     * @param context       chapter background
     * @param files         the scanned files of one chapter
     * @param resultsByFile per-file scan results keyed by walkthrough file id
     * @return cross-file risks, each tagged with the filename it manifests in
     */
    List<CrossFileRisk> detectCrossFile(ChapterContext context,
                                        List<WalkthroughFileEntity> files,
                                        Map<UUID, FileScanResult> resultsByFile);
}
