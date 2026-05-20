package com.altacod.favorites.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TelegramUserSyncPayloadParserTest {

    private final TelegramUserSyncPayloadParser parser =
            new TelegramUserSyncPayloadParser(new ObjectMapper().findAndRegisterModules());

    @Test
    void parsesMessagesAndMaxId() throws Exception {
        String json = """
                {
                  "messages": [
                    {"id": 101, "date": "2024-05-01T10:00:00Z", "text": "Whisper https://github.com/openai/whisper"},
                    {"id": 102, "date": "2024-05-02T11:00:00Z", "text": "   "}
                  ],
                  "maxId": 102
                }
                """;

        TelegramUserSyncPayload payload = parser.parse(json);

        assertEquals(1, payload.messages().size());
        assertEquals(101, payload.messages().get(0).id());
        assertEquals(102, payload.maxId());
        assertTrue(payload.messages().get(0).text().contains("whisper"));
    }
}
