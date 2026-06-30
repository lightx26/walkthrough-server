package com.pet.walkthroughserver.modules.riskzone.business.prompt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.walkthroughserver.modules._shared.infra.ai.exceptions.LlmResponseParseException;
import com.pet.walkthroughserver.modules.riskzone.business.models.CrossFileRisk;
import com.pet.walkthroughserver.modules.riskzone.business.models.DetectedRisk;
import com.pet.walkthroughserver.modules.riskzone.business.models.FileChangeSummary;
import com.pet.walkthroughserver.modules.riskzone.business.models.FileScanResult;
import com.pet.walkthroughserver.modules.riskzone.repository.RiskCategory;
import com.pet.walkthroughserver.modules.riskzone.repository.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RiskResponseParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Per-file (map) pass: parse the combined JSON object containing {@code risks} and
     * {@code change_summary}. Malformed individual risk entries are dropped with a warning;
     * a missing or empty {@code change_summary} yields {@link FileChangeSummary#empty()}.
     */
    public FileScanResult parseFileScan(String content) {
        if (content == null || content.isBlank()) {
            return new FileScanResult(List.of(), FileChangeSummary.empty());
        }

        String json = stripFences(content.trim());
        RawFileScan raw;
        try {
            raw = MAPPER.readValue(json, RawFileScan.class);
        } catch (Exception e) {
            throw new LlmResponseParseException(
                    "Cannot parse per-file LLM response as JSON object: " + e.getMessage(), e);
        }

        List<DetectedRisk> risks = new ArrayList<>();
        if (raw.risks() != null) {
            for (RawRisk r : raw.risks()) {
                try {
                    risks.add(toDetectedRisk(r));
                } catch (Exception e) {
                    log.warn("Dropping malformed risk entry: {} — {}", r, e.getMessage());
                }
            }
        }

        FileChangeSummary changeSummary = toChangeSummary(raw.changeSummary());
        return new FileScanResult(risks, changeSummary);
    }

    /**
     * Reduce pass: parse a JSON array where each risk additionally names the {@code filename}
     * it manifests in. Entries without a usable filename are dropped (a cross-file risk that
     * can't be attributed to a file cannot be persisted), as are malformed entries.
     */
    public List<CrossFileRisk> parseCrossFile(String content) {
        List<CrossFileRisk> results = new ArrayList<>();
        for (RawRisk r : readArray(content)) {
            try {
                if (r.filename() == null || r.filename().isBlank()) {
                    log.warn("Dropping cross-file risk without filename: {}", r);
                    continue;
                }
                results.add(new CrossFileRisk(r.filename().trim(), toDetectedRisk(r)));
            } catch (Exception e) {
                log.warn("Dropping malformed cross-file risk entry: {} — {}", r, e.getMessage());
            }
        }
        return results;
    }

    /** Shared, tolerant JSON-array deserialization. Throws only when the payload is unusable. */
    private List<RawRisk> readArray(String content) {
        if (content == null || content.isBlank()) return List.of();

        String json = stripFences(content.trim());
        try {
            return MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, RawRisk.class));
        } catch (Exception e) {
            throw new LlmResponseParseException("Cannot parse LLM response as JSON array: " + e.getMessage(), e);
        }
    }

    private DetectedRisk toDetectedRisk(RawRisk r) {
        if (r.title() == null || r.title().isBlank()) throw new IllegalArgumentException("missing title");
        if (r.description() == null || r.description().isBlank()) throw new IllegalArgumentException("missing description");

        RiskLevel level = parseLevel(r.riskLevel());
        RiskCategory category = RiskCategory.fromString(r.category());

        return new DetectedRisk(
                level, category,
                r.title().length() > 255 ? r.title().substring(0, 255) : r.title(),
                r.description(),
                r.suggestion(),
                r.startPosition(),
                r.endPosition(),
                normalizedSide(r.lineSide())
        );
    }

    private FileChangeSummary toChangeSummary(RawChangeSummary raw) {
        if (raw == null) return FileChangeSummary.empty();
        return new FileChangeSummary(
                raw.added() != null ? raw.added() : List.of(),
                raw.removed() != null ? raw.removed() : List.of(),
                raw.modified() != null ? raw.modified() : List.of()
        );
    }

    private RiskLevel parseLevel(String value) {
        if (value == null) return RiskLevel.LOW;
        try {
            return RiskLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RiskLevel.LOW;
        }
    }

    private String normalizedSide(String side) {
        if ("LEFT".equalsIgnoreCase(side) || "RIGHT".equalsIgnoreCase(side)) return side.toUpperCase();
        return null;
    }

    /** Strip ```json … ``` fences that some models emit despite json_mode. */
    private static String stripFences(String s) {
        if (s.startsWith("```")) {
            int first = s.indexOf('\n');
            int last = s.lastIndexOf("```");
            if (first >= 0 && last > first) {
                return s.substring(first + 1, last).trim();
            }
        }
        return s;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawFileScan(
            @JsonProperty("risks")          List<RawRisk> risks,
            @JsonProperty("change_summary") RawChangeSummary changeSummary
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawChangeSummary(
            @JsonProperty("added")    List<String> added,
            @JsonProperty("removed")  List<String> removed,
            @JsonProperty("modified") List<String> modified
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawRisk(
            @JsonProperty("risk_level")    String riskLevel,
            @JsonProperty("category")      String category,
            @JsonProperty("title")         String title,
            @JsonProperty("description")   String description,
            @JsonProperty("suggestion")    String suggestion,
            @JsonProperty("start_position") Integer startPosition,
            @JsonProperty("end_position")   Integer endPosition,
            @JsonProperty("line_side")     String lineSide,
            @JsonProperty("filename")      String filename
    ) {}
}
