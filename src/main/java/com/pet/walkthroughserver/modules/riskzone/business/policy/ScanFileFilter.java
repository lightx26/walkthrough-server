package com.pet.walkthroughserver.modules.riskzone.business.policy;

import com.pet.walkthroughserver.modules._shared.infra.ai.AiProperties;
import com.pet.walkthroughserver.modules._shared.infra.ai.AiProperties.ScanProperties;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughFileEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Guards the risk-scan prompt by deciding, per file, whether its diff is worth sending to the LLM.
 * Files that are generated (matched by {@link ScanProperties#getExcludePatterns()}) or whose patch
 * is too large are excluded so they never burn tokens or pollute the analysis.
 */
@Component
@RequiredArgsConstructor
public class ScanFileFilter {

    private final AiProperties aiProperties;

    /** Why a file was excluded, and whether the exclusion is worth surfacing to the user. */
    public record Decision(Type type, String reason) {
        public enum Type {
            /** Send the file to the LLM (possibly truncated downstream). */
            SCAN,
            /** Exclude and surface to the user with {@link #reason()}. */
            SKIP,
            /** Exclude quietly — nothing meaningful to analyze (e.g. binary / empty patch). */
            SKIP_SILENT
        }

        public boolean isScan()        { return type == Type.SCAN; }
        public boolean isVisibleSkip() { return type == Type.SKIP; }

        static Decision scan()              { return new Decision(Type.SCAN, null); }
        static Decision skip(String reason) { return new Decision(Type.SKIP, reason); }
        static Decision silent()            { return new Decision(Type.SKIP_SILENT, null); }
    }

    public Decision decide(WalkthroughFileEntity file) {
        String patch = file.getRawPatch();
        if (patch == null || patch.isBlank()) {
            return Decision.silent();
        }

        ScanProperties scan = aiProperties.getScan();

        if (matchesAnyPattern(file.getFilename(), scan.getExcludePatterns())) {
            return Decision.skip("Generated file excluded from analysis");
        }
        if (patch.length() > scan.getSkipPatchChars()) {
            return Decision.skip("File too large to analyze (" + (patch.length() / 1024) + " KB)");
        }
        return Decision.scan();
    }

    private static boolean matchesAnyPattern(String filename, List<String> patterns) {
        if (filename == null || patterns == null) return false;
        String basename = filename.contains("/")
                ? filename.substring(filename.lastIndexOf('/') + 1)
                : filename;
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) continue;
            if (globMatches(pattern, filename) || globMatches(pattern, basename)) return true;
        }
        return false;
    }

    /** Translate a simple glob ({@code *}, {@code ?}) into a regex and test it against {@code value}. */
    private static boolean globMatches(String glob, String value) {
        StringBuilder regex = new StringBuilder(glob.length() + 8);
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append('.');
                case '.', '\\', '+', '(', ')', '[', ']', '{', '}', '^', '$', '|' ->
                        regex.append('\\').append(c);
                default -> regex.append(c);
            }
        }
        return value.matches(regex.toString());
    }
}
