package com.altacod.favorites.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String timezone,
        String projectRoot,
        Sync sync,
        OpenAi openai,
        Telegram telegram,
        GitHub github,
        CategoryConsolidation categoryConsolidation
) {
    public record Sync(String cron, int processBatchSize) {}

    public record OpenAi(String apiKey, String model, boolean enabled) {}

    public record CategoryConsolidation(boolean enabled, int maxCategories, int batchSize) {}

    public record Telegram(
            String botToken,
            boolean enabled,
            UserApi userApi
    ) {}

    public record UserApi(
            boolean enabled,
            String apiId,
            String apiHash,
            String sessionPath,
            String pythonExecutable,
            String syncScript,
            int messageLimit
    ) {}

    public record GitHub(String token) {}
}
