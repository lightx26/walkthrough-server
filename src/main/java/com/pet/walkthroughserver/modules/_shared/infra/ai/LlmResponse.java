package com.pet.walkthroughserver.modules._shared.infra.ai;

public record LlmResponse(
        String content,
        String model,
        int promptTokens,
        int completionTokens
) {}
