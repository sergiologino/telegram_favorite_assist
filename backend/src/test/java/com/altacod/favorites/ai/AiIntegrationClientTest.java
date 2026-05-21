package com.altacod.favorites.ai;

import com.altacod.favorites.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiIntegrationClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiIntegrationClient client = new AiIntegrationClient(
            new AppProperties(
                    "UTC",
                    "..",
                    new AppProperties.Sync("0 0 8,20 * * *", 50),
                    new AppProperties.OpenAi("", "gpt-4o-mini", false),
                    new AppProperties.AiIntegration(false, "", "", "finds-catalog", "", "admin", "", "admin@example.com"),
                    new AppProperties.Telegram("", false, new AppProperties.UserApi(
                            false, "", "", "./data/telegram.session", "python", "scripts/sync_saved_messages.py", 500
                    )),
                    new AppProperties.GitHub(""),
                    new AppProperties.CategoryConsolidation(false, 20, 15)
            ),
            new AiIntegrationOwnerBootstrap(
                    new AppProperties(
                            "UTC",
                            "..",
                            new AppProperties.Sync("0 0 8,20 * * *", 50),
                            new AppProperties.OpenAi("", "gpt-4o-mini", false),
                            new AppProperties.AiIntegration(false, "", "", "finds-catalog", "", "admin", "", "admin@example.com"),
                            new AppProperties.Telegram("", false, new AppProperties.UserApi(
                                    false, "", "", "./data/telegram.session", "python", "scripts/sync_saved_messages.py", 500
                            )),
                            new AppProperties.GitHub(""),
                            new AppProperties.CategoryConsolidation(false, 20, 15)
                    ),
                    null,
                    objectMapper
            ),
            null,
            objectMapper
    );

    @Test
    void parsesSuccessfulIntegrationResponse() throws Exception {
        JsonNode integrationResponse = objectMapper.readTree("""
                {
                  "status": "success",
                  "response": {
                    "choices": [
                      {
                        "message": {
                          "content": "{\\"categories\\":[\\"Бизнес\\",\\"Разработка\\"]}"
                        }
                      }
                    ]
                  }
                }
                """);

        JsonNode parsed = client.parseChatJsonResponse(integrationResponse);

        assertEquals("Бизнес", parsed.get("categories").get(0).asText());
        assertEquals("Разработка", parsed.get("categories").get(1).asText());
    }

    @Test
    void failsOnIntegrationErrorStatus() throws Exception {
        JsonNode integrationResponse = objectMapper.readTree("""
                {
                  "status": "failed",
                  "errorMessage": "Network not found"
                }
                """);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> client.parseChatJsonResponse(integrationResponse)
        );
        assertEquals("AI integration request failed: Network not found", ex.getMessage());
    }
}
