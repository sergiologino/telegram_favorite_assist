package com.altacod.favorites.ai;

import com.altacod.favorites.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Как в AltaPens: admin JWT → привязка клиента по API-ключу к владельцу ({@code POST /api/admin/clients/{id}/assign-user}).
 * Для {@code /api/ai/process} достаточно {@code X-API-Key}; bootstrap нужен для лимитов подписки.
 */
@Service
public class AiIntegrationOwnerBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AiIntegrationOwnerBootstrap.class);

    private final AppProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean linked = new AtomicBoolean(false);

    public AiIntegrationOwnerBootstrap(AppProperties properties, RestClient restClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureLinked();
    }

    public void ensureLinked() {
        if (!isEnabled()) {
            return;
        }
        if (linked.get()) {
            return;
        }
        synchronized (this) {
            if (linked.get()) {
                return;
            }
            try {
                runOnce();
                linked.set(true);
            } catch (RestClientResponseException ex) {
                log.warn(
                        "AI integration: client link to {} failed ({}): {}",
                        properties.aiIntegration().ownerEmail(),
                        ex.getStatusCode().value(),
                        responseError(ex)
                );
            } catch (RuntimeException ex) {
                log.warn("AI integration: client link failed: {}", ex.getMessage());
            }
        }
    }

    boolean isEnabled() {
        AppProperties.AiIntegration ai = properties.aiIntegration();
        return ai != null
                && ai.enabled()
                && StringUtils.hasText(ai.baseUrl())
                && StringUtils.hasText(ai.apiKey())
                && StringUtils.hasText(ai.adminPassword());
    }

    private void runOnce() {
        AppProperties.AiIntegration ai = properties.aiIntegration();
        String baseUrl = AiIntegrationUrls.normalizeBaseUrl(ai.baseUrl());

        ObjectNode loginBody = objectMapper.createObjectNode();
        loginBody.put("username", StringUtils.hasText(ai.adminUsername()) ? ai.adminUsername() : "admin");
        loginBody.put("password", ai.adminPassword());

        JsonNode loginResponse = restClient.post()
                .uri(baseUrl + "/api/auth/login")
                .header("Content-Type", "application/json")
                .body(loginBody)
                .retrieve()
                .body(JsonNode.class);

        if (loginResponse == null || !loginResponse.hasNonNull("token")) {
            throw new IllegalStateException("integration login: no token in response");
        }
        String token = loginResponse.get("token").asText();

        JsonNode clients = restClient.get()
                .uri(baseUrl + "/api/admin/clients")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(JsonNode.class);

        if (clients == null || !clients.isArray()) {
            throw new IllegalStateException("integration: empty GET /api/admin/clients");
        }

        String wantKey = ai.apiKey().trim();
        String clientId = null;
        for (JsonNode row : clients) {
            String apiKey = row.path("apiKey").asText("");
            if (wantKey.equals(apiKey.trim())) {
                clientId = row.path("id").asText(null);
                break;
            }
        }
        if (!StringUtils.hasText(clientId)) {
            throw new IllegalStateException(
                    "integration: no client in GET /api/admin/clients matches AI_INTEGRATION_API_KEY"
            );
        }

        ObjectNode assignBody = objectMapper.createObjectNode();
        assignBody.put("userEmail", ai.ownerEmail());

        restClient.post()
                .uri(baseUrl + "/api/admin/clients/" + clientId + "/assign-user")
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(assignBody)
                .retrieve()
                .toBodilessEntity();

        log.info("AI integration: client {} linked to owner {}", clientId, ai.ownerEmail());
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
}
