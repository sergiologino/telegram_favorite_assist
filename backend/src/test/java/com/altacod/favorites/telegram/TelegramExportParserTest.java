package com.altacod.favorites.telegram;

import com.altacod.favorites.domain.PostSource;
import com.altacod.favorites.domain.PostStatus;
import com.altacod.favorites.domain.TelegramPostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(TelegramExportParser.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TelegramExportParserTest {

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:mem:TelegramExportParserTest");
    }

    @Autowired
    private TelegramExportParser parser;

    @Autowired
    private TelegramPostRepository postRepository;

    @Test
    void importsMessagesFromExport() throws Exception {
        String json = """
                {
                  "name": "Saved Messages",
                  "messages": [
                    {
                      "id": 100,
                      "type": "message",
                      "date": "2024-05-01T10:00:00",
                      "text": "Whisper https://github.com/openai/whisper"
                    },
                    {
                      "id": 101,
                      "type": "message",
                      "date": "2024-05-02T11:00:00",
                      "text": ["Link ", {"type": "link", "text": "https://example.com"}]
                    },
                    {
                      "id": 102,
                      "type": "message",
                      "date": "2024-05-03T12:00:00",
                      "media_type": "web_page",
                      "text": "",
                      "title": "Whisper",
                      "description": "Speech recognition",
                      "href": "https://github.com/openai/whisper"
                    },
                    {
                      "id": 103,
                      "type": "message",
                      "date": "2024-05-04T13:00:00",
                      "text": [
                        "Подкаст ",
                        {"type": "text_link", "text": "Design Kit", "href": "https://podfm.ru/podcasts/dizajn-kit/"}
                      ]
                    }
                  ]
                }
                """;

        TelegramExportParser.ImportResult result = parser.importExport(new ObjectMapper().readTree(json));

        assertEquals(4, result.imported());
        assertEquals(0, result.skippedDuplicate());
        assertEquals(0, result.skippedEmpty());
        assertEquals(4, postRepository.count());

        var post = postRepository.findByTelegramMessageId(100L).orElseThrow();
        assertEquals(PostSource.EXPORT, post.getSource());
        assertEquals(PostStatus.PENDING, post.getStatus());
        assertTrue(post.getTextContent().contains("whisper"));

        var webPagePost = postRepository.findByTelegramMessageId(102L).orElseThrow();
        assertTrue(webPagePost.getTextContent().contains("https://github.com/openai/whisper"));

        var textLinkPost = postRepository.findByTelegramMessageId(103L).orElseThrow();
        assertTrue(textLinkPost.getTextContent().contains("https://podfm.ru/podcasts/dizajn-kit/"));
    }
}
