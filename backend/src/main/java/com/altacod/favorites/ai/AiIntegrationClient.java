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
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;
import java.util.UUID;

/**
 * Клиент noteapp-ai-integration ({@code POST /api/ai/process}).
 * <p>
 * Авторизация вызовов AI — только {@code X-API-Key}. Логин/пароль портального пользователя
 * ({@code /api/user/auth/register|login}) нужны для привязки к аккаунту и получения стабильного
 * {@code userId} во внешней системе (если не задан {@code AI_INTEGRATION_USER_ID}).
 */
@Service
public class AiIntegrationClient {

    private static final Logger log = LoggerFactory.getLogger(AiIntegrationClient.class);

    private final AppProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private volatile String resolvedExternalUserId;
    private final Object userIdLock = new Object();

    public AiIntegrationClient(AppProperties properties, RestClient restClient, ObjectMapper objectMapper) {
        this.properties = properties;
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
        request.put("userId", resolveExternalUserId());
        request.put("requestType", "chat");
        if (StringUtils.hasText(properties.aiIntegration().networkName())) {
            request.put("networkName", properties.aiIntegration().networkName());
        }
        request.set("payload", payload);

        JsonNode response = restClient.post()
                .uri(normalizeBaseUrl(properties.aiIntegration().baseUrl()) + "/api/ai/process")
                .header("X-API-Key", properties.aiIntegration().apiKey())
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        return parseChatJsonResponse(response);
    }

    private String resolveExternalUserId() {
        if (resolvedExternalUserId != null) {
            return resolvedExternalUserId;
        }
        synchronized (userIdLock) {
            if (resolvedExternalUserId != null) {
                return resolvedExternalUserId;
            }

            AppProperties.AiIntegration ai = properties.aiIntegration();
            if (StringUtils.hasText(ai.externalUserId())) {
                resolvedExternalUserId = ai.externalUserId().trim();
                log.info("AI integration: using configured external userId");
                return resolvedExternalUserId;
            }

            if (!StringUtils.hasText(ai.userEmail()) || !StringUtils.hasText(ai.userPassword())) {
                throw new IllegalStateException(
                        "AI integration requires AI_INTEGRATION_USER_EMAIL and AI_INTEGRATION_USER_PASSWORD "
                                + "or AI_INTEGRATION_USER_ID"
                );
            }

            resolvedExternalUserId = registerOrLoginPortalUser(ai).toString();
            log.info("AI integration: portal user ready, external userId={}", resolvedExternalUserId);
            return resolvedExternalUserId;
        }
    }

    private UUID registerOrLoginPortalUser(AppProperties.AiIntegration ai) {
        String baseUrl = normalizeBaseUrl(ai.baseUrl());
        String email = ai.userEmail().trim().toLowerCase();
        String password = ai.userPassword();
        String fullName = StringUtils.hasText(ai.userFullName()) ? ai.userFullName().trim() : "Finds";

        Optional<UUID> userId = tryPortalLogin(baseUrl, email, password);
        if (userId.isPresent()) {
            log.info("AI integration: portal user logged in ({})", email);
            return userId.get();
        }

        userId = tryPortalRegister(baseUrl, email, password, fullName);
        if (userId.isPresent()) {
            log.info("AI integration: portal user registered ({})", email);
            return userId.get();
        }

        userId = tryPortalLogin(baseUrl, email, password);
        if (userId.isPresent()) {
            log.info("AI integration: portal user logged in after register conflict ({})", email);
            return userId.get();
        }

        throw new IllegalStateException(
                "Portal user auth failed for " + email
                        + ": could not register or login. Check AI_INTEGRATION_USER_EMAIL/PASSWORD."
        );
    }

    private Optional<UUID> tryPortalLogin(String baseUrl, String email, String password) {
        ObjectNode loginBody = objectMapper.createObjectNode();
        loginBody.put("email", email);
        loginBody.put("password", password);

        try {
            JsonNode loginResponse = restClient.post()
                    .uri(baseUrl + "/api/user/auth/login")
                    .header("Content-Type", "application/json")
                    .body(loginBody)
                    .retrieve()
                    .body(JsonNode.class);
            return extractUserId(loginResponse);
        } catch (RestClientResponseException ex) {
            log.debug("Portal login failed ({}): {}", ex.getStatusCode().value(), responseError(ex));
            return Optional.empty();
        }
    }

    private Optional<UUID> tryPortalRegister(String baseUrl, String email, String password, String fullName) {
        ObjectNode registerBody = objectMapper.createObjectNode();
        registerBody.put("email", email);
        registerBody.put("password", password);
        registerBody.put("repeatPassword", password);
        registerBody.put("fullName", fullName);

        try {
            JsonNode registerResponse = restClient.post()
                    .uri(baseUrl + "/api/user/auth/register")
                    .header("Content-Type", "application/json")
                    .body(registerBody)
                    .retrieve()
                    .body(JsonNode.class);
            return extractUserId(registerResponse);
        } catch (RestClientResponseException ex) {
            log.debug("Portal register failed ({}): {}", ex.getStatusCode().value(), responseError(ex));
            return Optional.empty();
        }
    }

    private Optional<UUID> extractUserId(JsonNode response) {
        if (response != null && response.hasNonNull("userId")) {
            return Optional.of(UUID.fromString(response.get("userId").asText()));
        }
        return Optional.empty();
    }

    private String responseError(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (!StringUtils.hasText(body)) {
            return ex.getMessage();
        }
        try {
            JsonNode parsed = objectMapper.readTree(body);
            String error = parsed.path("error").asText("");
            if (StringUtils.hasText(error)) {
                return error;
            }
        } catch (Exception ignored) {
            // keep raw body
        }
        return body;
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

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
