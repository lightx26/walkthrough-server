package com.pet.walkthroughserver.modules.riskzone.business.models;

import java.util.List;

/**
 * Semantic summary of what changed in a file, produced by the per-file (map) LLM pass.
 * Intentionally limited to the public/exported surface so the cross-file (reduce) pass
 * can reason about contract mismatches without seeing raw diff content.
 */
public record FileChangeSummary(
        List<String> added,
        List<String> removed,
        List<String> modified
) {
    public static FileChangeSummary empty() {
        return new FileChangeSummary(List.of(), List.of(), List.of());
    }

    public boolean isEmpty() {
        return (added == null || added.isEmpty())
                && (removed == null || removed.isEmpty())
                && (modified == null || modified.isEmpty());
    }
}
