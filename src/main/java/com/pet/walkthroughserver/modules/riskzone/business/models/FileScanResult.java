package com.pet.walkthroughserver.modules.riskzone.business.models;

import java.util.List;

/**
 * Output of the per-file (map) LLM pass: the detected risks and a semantic summary of
 * what changed in the file. The summary is forwarded to the cross-file (reduce) pass so
 * it can spot contract mismatches across files without receiving raw diff content.
 */
public record FileScanResult(
        List<DetectedRisk> risks,
        FileChangeSummary changeSummary
) {}
