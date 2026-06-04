package com.pet.walkthroughserver.modules._shared.infra.ai;

public record LlmRequest(
        String systemPrompt,
        String userPrompt,
        String model,
        Integer maxTokens,
        Double temperature,
        boolean jsonMode
) {
    public static LlmRequest of(String systemPrompt, String userPrompt) {
        return new LlmRequest(systemPrompt, userPrompt, null, null, null, true);
    }
}
