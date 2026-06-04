package com.pet.walkthroughserver.modules._shared.infra.ai;

/**
 * Port for LLM chat-completion calls.
 * Business code depends only on this interface — never on a provider class.
 * To swap providers: set app.ai.provider and supply credentials. No business code changes.
 */
public interface LlmClient {

    LlmResponse complete(LlmRequest request);

    /** Identifier stored on scan records for telemetry, e.g. "deepseek". */
    String providerName();
}
