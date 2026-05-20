package com.altacod.favorites.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramUserSyncPayloadParser {

    private final ObjectMapper objectMapper;

    public TelegramUserSyncPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TelegramUserSyncPayload parse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<TelegramUserSyncPayload.Message> messages = new ArrayList<>();

        JsonNode messagesNode = root.path("messages");
        if (messagesNode.isArray()) {
            for (JsonNode node : messagesNode) {
                String text = node.path("text").asText("").trim();
                if (text.isBlank()) {
                    continue;
                }
                messages.add(new TelegramUserSyncPayload.Message(
                        node.path("id").asLong(),
                        Instant.parse(node.path("date").asText()),
                        text
                ));
            }
        }

        long maxId = root.path("maxId").asLong(0);
        if (maxId == 0 && !messages.isEmpty()) {
            maxId = messages.stream()
                    .mapToLong(TelegramUserSyncPayload.Message::id)
                    .max()
                    .orElse(0);
        }

        return new TelegramUserSyncPayload(messages, maxId);
    }
}
