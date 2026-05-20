package com.altacod.favorites.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TelegramExportContentTest {

    private TelegramExportParser parser;

    @BeforeEach
    void setUp() {
        parser = new TelegramExportParser(null);
    }

    @Test
    void extractsTextLinkHrefFromFormattedText() throws Exception {
        String json = """
                {
                  "text": [
                    "Подкаст ",
                    {"type": "text_link", "text": "Дизайн Кит", "href": "https://podfm.ru/podcasts/dizajn-kit/"}
                  ]
                }
                """;

        String content = parser.extractImportableContent(new ObjectMapper().readTree(json));

        assertTrue(content.contains("Дизайн Кит"));
        assertTrue(content.contains("https://podfm.ru/podcasts/dizajn-kit/"));
    }

    @Test
    void extractsWebPageMessageWithoutPlainText() throws Exception {
        String json = """
                {
                  "media_type": "web_page",
                  "text": "",
                  "title": "OpenAI Whisper",
                  "description": "Speech recognition system",
                  "href": "https://github.com/openai/whisper"
                }
                """;

        String content = parser.extractImportableContent(new ObjectMapper().readTree(json));

        assertTrue(content.contains("OpenAI Whisper"));
        assertTrue(content.contains("Speech recognition system"));
        assertTrue(content.contains("https://github.com/openai/whisper"));
    }

    @Test
    void normalizesBareLinkEntity() throws Exception {
        String json = """
                {
                  "text": [
                    "сайт ",
                    {"type": "link", "text": "example.com"}
                  ]
                }
                """;

        String content = parser.extractImportableContent(new ObjectMapper().readTree(json));

        assertTrue(content.contains("https://example.com"));
    }
}
