package com.pet.walkthroughserver.modules._shared.infra.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private String provider = "deepseek";
    private boolean enabled = true;

    private DeepSeekProperties deepseek = new DeepSeekProperties();
    private ScanProperties scan = new ScanProperties();

    @Getter
    @Setter
    public static class DeepSeekProperties {
        private String baseUrl = "https://api.deepseek.com";
        private String apiKey = "";
        private String model = "deepseek-chat";
        private String connectTimeout = "5s";
        private String readTimeout = "60s";
        private int maxTokens = 2048;
        private double temperature = 0.1;
    }

    @Getter
    @Setter
    public static class ScanProperties {
        private int maxFiles = 30;
        private int windowContextLines = 3;
        private int maxWindowChars = 6000;

        /**
         * Filename/glob patterns treated as generated content that should never be sent to the
         * LLM (lockfiles, minified bundles, source maps, generated stubs, …). Patterns are matched
         * against both the full path and the basename; {@code *} and {@code ?} wildcards are supported.
         */
        private List<String> excludePatterns = new ArrayList<>(List.of(
                "package-lock.json", "yarn.lock", "pnpm-lock.yaml", "npm-shrinkwrap.json",
                "composer.lock", "Gemfile.lock", "poetry.lock", "Cargo.lock", "go.sum",
                "*.min.js", "*.min.css", "*.map", "*.lock", "*.snap",
                "*.pb.go", "*_pb2.py", "*.generated.*"
        ));

        /**
         * Files whose raw patch exceeds this length are truncated (tail dropped) before being sent
         * to the LLM — "quite large" files are still analyzed, just trimmed.
         */
        private int maxPatchChars = 20000;

        /**
         * Files whose raw patch exceeds this length are skipped entirely and treated as generated —
         * "way too large" files are never sent to the LLM and are surfaced to the user as not scanned.
         */
        private int skipPatchChars = 100000;

        /** Master switch for the chapter-level (reduce) cross-file analysis pass. */
        private boolean crossFileAnalysis = true;
        /** Skip the reduce pass for chapters with fewer than this many scanned files. */
        private int minFilesForCrossFile = 2;
        /** Truncate each walkthrough/chapter description injected as context. */
        private int maxContextChars = 1500;
    }
}
