package com.altacod.favorites.ai;

import com.altacod.favorites.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiIntegrationClientAuthTest {

    private MockRestServiceServer server;
    private AiIntegrationClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        ObjectMapper objectMapper = new ObjectMapper();
        AppProperties properties = new AppProperties(
                "UTC",
                "..",
                new AppProperties.Sync("0 0 8,20 * * *", 50),
                new AppProperties.OpenAi("", "gpt-4o-mini", false),
                new AppProperties.AiIntegration(
                        true,
                        "https://ai.example.com",
                        "aikey_test",
                        "finds-catalog",
                        "",
                        "admin",
                        "",
                        "admin@example.com"
                ),
                new AppProperties.Telegram("", false, new AppProperties.UserApi(
                        false, "", "", "./data/telegram.session", "python", "scripts/sync_saved_messages.py", 500
                )),
                new AppProperties.GitHub(""),
                new AppProperties.CategoryConsolidation(false, 20, 15)
        );
        client = new AiIntegrationClient(
                properties,
                new AiIntegrationOwnerBootstrap(properties, builder.build(), objectMapper),
                builder.build(),
                objectMapper
        );
    }

    @AfterEach
    void verify() {
        server.verify();
    }

    @Test
    void callsProcessWithApiKeyAndExternalUserId() throws Exception {
        server.expect(requestTo("https://ai.example.com/api/ai/process"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "status": "success",
                          "response": {
                            "choices": [
                              {"message": {"content": "{\\"categories\\":[\\"Разработка\\"]}"}}
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        assertEquals("Разработка", client.chatJson("system", "user").get("categories").get(0).asText());
    }
}
