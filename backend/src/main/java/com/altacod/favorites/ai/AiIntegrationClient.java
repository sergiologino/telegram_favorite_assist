package com.altacod.favorites.ai;

import com.altacod.favorites.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Клиент noteapp-ai-integration ({@code POST /api/ai/process}).
 * Авторизация — только {@code X-API-Key}. {@code userId} — произвольная строка во внешней системе.
 */
@Service
public class AiIntegrationClient {

    private static final Logger log = LoggerFactory.getLogger(AiIntegrationClient.class);
    private static final String DEFAULT_EXTERNAL_USER_ID = "finds-catalog";

    private final AppProperties properties;
    private final AiIntegrationOwnerBootstrap ownerBootstrap;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AiIntegrationClient(
            AppProperties properties,
            AiIntegrationOwnerBootstrap ownerBootstrap,
            RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.ownerBootstrap = ownerBootstrap;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        AppProperties.AiIntegration ai = properties.aiIntegration();
        return ai != null
                && ai.enabled()
                && StringUtils.hasText(ai.baseUrl())
                && StringUtils.hasText(ai.apiKey());
    }

    public JsonNode chatJson(String systemPrompt, String userPrompt) throws Exception {
        if (!isEnabled()) {
            throw new IllegalStateException("AI integration is not configured");
        }

        ownerBootstrap.ensureLinked();

        ObjectNode payload = objectMapper.createObjectNode();
        if (StringUtils.hasText(properties.openai().model())) {
            payload.put("model", properties.openai().model());
        }
        payload.set("response_format", objectMapper.createObjectNode().put("type", "json_object"));
        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(objectMapper.createObjectNode().put("role", "system").put("content", systemPrompt));
        messages.add(objectMapper.createObjectNode().put("role", "user").put("content", userPrompt));
        payload.set("messages", messages);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("userId", externalUserId());
        request.put("requestType", "chat");
        if (StringUtils.hasText(properties.aiIntegration().networkName())) {
            request.put("networkName", properties.aiIntegration().networkName());
        }
        request.set("payload", payload);

        JsonNode response = restClient.post()
                .uri(AiIntegrationUrls.normalizeBaseUrl(properties.aiIntegration().baseUrl()) + "/api/ai/process")
                .header("X-API-Key", properties.aiIntegration().apiKey())
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        return parseChatJsonResponse(response);
    }

    private String externalUserId() {
        AppProperties.AiIntegration ai = properties.aiIntegration();
        if (StringUtils.hasText(ai.externalUserId())) {
            return ai.externalUserId().trim();
        }
        log.debug("AI integration: using default external userId {}", DEFAULT_EXTERNAL_USER_ID);
        return DEFAULT_EXTERNAL_USER_ID;
    }

    JsonNode parseChatJsonResponse(JsonNode integrationResponse) throws Exception {
        if (integrationResponse == null) {
            throw new IllegalStateException("Empty AI integration response");
        }

        String status = integrationResponse.path("status").asText("");
        if (!"success".equalsIgnoreCase(status)) {
            String error = integrationResponse.path("errorMessage").asText("unknown error");
            throw new IllegalStateException("AI integration request failed: " + error);
        }

        JsonNode providerResponse = integrationResponse.get("response");
        if (providerResponse == null || !providerResponse.has("choices") || providerResponse.get("choices").isEmpty()) {
            throw new IllegalStateException("Empty provider response from AI integration");
        }

        String content = providerResponse.get("choices").get(0).get("message").get("content").asText();
        return objectMapper.readTree(content);
    }
}
