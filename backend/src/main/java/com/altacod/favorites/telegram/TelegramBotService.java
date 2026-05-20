package com.altacod.favorites.telegram;

import com.altacod.favorites.config.AppProperties;
import com.altacod.favorites.domain.PostSource;
import com.altacod.favorites.domain.PostStatus;
import com.altacod.favorites.domain.SyncState;
import com.altacod.favorites.domain.SyncStateRepository;
import com.altacod.favorites.domain.TelegramPost;
import com.altacod.favorites.domain.TelegramPostRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TelegramBotService {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);

    private final AppProperties properties;
    private final RestClient restClient;
    private final SyncStateRepository syncStateRepository;
    private final TelegramPostRepository postRepository;

    public TelegramBotService(
            AppProperties properties,
            RestClient restClient,
            SyncStateRepository syncStateRepository,
            TelegramPostRepository postRepository
    ) {
        this.properties = properties;
        this.restClient = restClient;
        this.syncStateRepository = syncStateRepository;
        this.postRepository = postRepository;
    }

    public boolean isConfigured() {
        return properties.telegram().enabled()
                && properties.telegram().botToken() != null
                && !properties.telegram().botToken().isBlank();
    }

    @Transactional
    public BotSyncResult syncUpdates() {
        if (!isConfigured()) {
            return new BotSyncResult(0, 0, "Telegram bot token is not configured");
        }

        SyncState state = syncStateRepository.findById(1L).orElseGet(() -> {
            SyncState created = new SyncState();
            created.setId(1L);
            created.setLastUpdateId(0L);
            return syncStateRepository.save(created);
        });

        int imported = 0;
        int skipped = 0;
        long nextOffset = state.getLastUpdateId() + 1;

        while (true) {
            final long requestOffset = nextOffset;
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.telegram.org")
                            .path("/bot" + properties.telegram().botToken() + "/getUpdates")
                            .queryParam("offset", requestOffset)
                            .queryParam("timeout", 0)
                            .queryParam("allowed_updates", "[\"message\"]")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.path("ok").asBoolean(false)) {
                String error = response == null ? "empty response" : response.path("description").asText("unknown error");
                return new BotSyncResult(imported, skipped, error);
            }

            JsonNode results = response.path("result");
            if (!results.isArray() || results.isEmpty()) {
                break;
            }

            for (JsonNode update : results) {
                long updateId = update.path("update_id").asLong();
                nextOffset = updateId + 1;
                state.setLastUpdateId(updateId);

                JsonNode message = update.path("message");
                if (message.isMissingNode()) {
                    continue;
                }

                long messageId = message.path("message_id").asLong();
                if (postRepository.findByTelegramMessageId(messageId).isPresent()) {
                    skipped++;
                    continue;
                }

                String text = extractMessageText(message);
                if (text.isBlank()) {
                    skipped++;
                    continue;
                }

                TelegramPost post = new TelegramPost();
                post.setTelegramMessageId(messageId);
                post.setTextContent(text);
                post.setPostedAt(Instant.ofEpochSecond(message.path("date").asLong()));
                post.setSource(PostSource.BOT);
                post.setStatus(PostStatus.PENDING);
                postRepository.save(post);
                imported++;
            }
        }

        syncStateRepository.save(state);
        log.info("Telegram bot sync finished: imported={}, skipped={}", imported, skipped);
        return new BotSyncResult(imported, skipped, null);
    }

    String extractMessageText(JsonNode message) {
        StringBuilder builder = new StringBuilder();

        String text = message.path("text").asText("");
        if (!text.isBlank()) {
            builder.append(text);
        }

        String caption = message.path("caption").asText("");
        if (!caption.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(caption);
        }

        appendForwarded(builder, message.path("forward_from_chat"), message.path("forward_date"));
        return builder.toString().trim();
    }

    private void appendForwarded(StringBuilder builder, JsonNode forwardFromChat, JsonNode forwardDate) {
        if (forwardFromChat.isMissingNode()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append("[forwarded");
        String title = forwardFromChat.path("title").asText("");
        if (!title.isBlank()) {
            builder.append(" from ").append(title);
        }
        builder.append(']');
    }

    public record BotSyncResult(int imported, int skipped, String error) {
        public boolean success() {
            return error == null;
        }
    }
}
