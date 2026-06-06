package com.pet.walkthroughserver.modules._shared.infra.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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

        /** Master switch for the chapter-level (reduce) cross-file analysis pass. */
        private boolean crossFileAnalysis = true;
        /** Skip the reduce pass for chapters with fewer than this many scanned files. */
        private int minFilesForCrossFile = 2;
        /** Truncate each walkthrough/chapter description injected as context. */
        private int maxContextChars = 1500;
    }
}
