package com.altacod.favorites.enrichment;

import com.altacod.favorites.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GitHubServiceTest {

    private final GitHubService gitHubService = new GitHubService(
            RestClient.builder().build(),
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
            )
    );

    @Test
    void parsesGitHubRepoUrl() {
        Optional<String[]> parts = gitHubService.parseRepo("https://github.com/openai/whisper");
        assertTrue(parts.isPresent());
        assertEquals("openai", parts.get()[0]);
        assertEquals("whisper", parts.get()[1]);
    }
}
