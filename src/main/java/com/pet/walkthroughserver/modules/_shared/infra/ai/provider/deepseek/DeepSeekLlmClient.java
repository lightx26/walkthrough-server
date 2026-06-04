package com.pet.walkthroughserver.modules._shared.infra.ai.provider.deepseek;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pet.walkthroughserver.modules._shared.infra.ai.AiProperties;
import com.pet.walkthroughserver.modules._shared.infra.ai.LlmClient;
import com.pet.walkthroughserver.modules._shared.infra.ai.LlmRequest;
import com.pet.walkthroughserver.modules._shared.infra.ai.LlmResponse;
import com.pet.walkthroughserver.modules._shared.infra.ai.exceptions.LlmApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek implementation of LlmClient using the OpenAI-compatible chat completions API.
 * Active when app.ai.provider=deepseek (the default).
 *
 * To switch providers: set app.ai.provider=anthropic (or openai) and supply the
 * corresponding credentials. This bean deactivates; the matching provider bean activates.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "deepseek", matchIfMissing = true)
public class DeepSeekLlmClient implements LlmClient {

    private final RestClient restClient;
    private final AiProperties.DeepSeekProperties props;

    public DeepSeekLlmClient(RestClient.Builder builder, AiProperties aiProperties) {
        this.props = aiProperties.getDeepseek();
        this.restClient = builder
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String model = request.model() != null ? request.model() : props.getModel();
        int maxTokens = request.maxTokens() != null ? request.maxTokens() : props.getMaxTokens();
        double temperature = request.temperature() != null ? request.temperature() : props.getTemperature();

        ChatRequest body = buildRequest(request, model, maxTokens, temperature);

        try {
            ChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(ChatResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new LlmApiException("DeepSeek returned an empty response");
            }

            String content = response.choices().get(0).message().content();
            String usedModel = response.model() != null ? response.model() : model;
            int promptTokens = response.usage() != null ? response.usage().promptTokens() : 0;
            int completionTokens = response.usage() != null ? response.usage().completionTokens() : 0;

            log.info("DeepSeek completed: model={}, prompt_tokens={}, completion_tokens={}",
                    usedModel, promptTokens, completionTokens);

            return new LlmResponse(content, usedModel, promptTokens, completionTokens);

        } catch (RestClientResponseException ex) {
            log.error("DeepSeek API error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new LlmApiException("DeepSeek API error: " + ex.getStatusCode(), ex);
        } catch (ResourceAccessException ex) {
            log.error("DeepSeek connection error: {}", ex.getMessage());
            throw new LlmApiException("DeepSeek connection failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String providerName() {
        return "deepseek";
    }

    private ChatRequest buildRequest(LlmRequest request, String model, int maxTokens, double temperature) {
        List<Message> messages = List.of(
                new Message("system", request.systemPrompt()),
                new Message("user", request.userPrompt())
        );
        Map<String, String> responseFormat = request.jsonMode()
                ? Map.of("type", "json_object")
                : null;
        return new ChatRequest(model, messages, maxTokens, temperature, responseFormat);
    }

    // ── Wire-format records ──

    record ChatRequest(
            String model,
            List<Message> messages,
            @JsonProperty("max_tokens") int maxTokens,
            double temperature,
            @JsonProperty("response_format") Map<String, String> responseFormat
    ) {}

    record Message(String role, String content) {}

    record ChatResponse(
            String model,
            List<Choice> choices,
            Usage usage
    ) {}

    record Choice(Message message) {}

    record Usage(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens
    ) {}
}
