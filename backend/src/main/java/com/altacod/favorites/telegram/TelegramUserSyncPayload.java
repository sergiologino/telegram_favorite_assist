package com.altacod.favorites.telegram;

import java.time.Instant;
import java.util.List;

public record TelegramUserSyncPayload(
        List<Message> messages,
        long maxId
) {
    public record Message(
            long id,
            Instant date,
            String text
    ) {
    }
}
