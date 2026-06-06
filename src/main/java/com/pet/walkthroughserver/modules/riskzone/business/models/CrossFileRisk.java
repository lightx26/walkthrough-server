package com.pet.walkthroughserver.modules.riskzone.business.models;

/**
 * A risk produced by the chapter-level (reduce) pass. Unlike a per-file {@link DetectedRisk},
 * it carries the {@code filename} the LLM attributed it to, because the reduce prompt sees
 * multiple files at once. The handler maps {@code filename} back to a walkthrough file id;
 * risks naming an unknown file are dropped rather than guessed at.
 *
 * @param filename the file the risk primarily manifests in (as reported by the model)
 * @param risk     the parsed risk payload
 */
public record CrossFileRisk(
        String filename,
        DetectedRisk risk
) {}
