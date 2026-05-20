package com.altacod.favorites.telegram;

import com.altacod.favorites.domain.PostSource;
import com.altacod.favorites.domain.PostStatus;
import com.altacod.favorites.domain.SyncStateRepository;
import com.altacod.favorites.domain.TelegramPostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({TelegramSavedMessagesImporter.class})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TelegramSavedMessagesImporterTest {

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:mem:TelegramSavedMessagesImporterTest");
    }

    @Autowired
    private TelegramSavedMessagesImporter importer;

    @Autowired
    private TelegramPostRepository postRepository;

    @Autowired
    private SyncStateRepository syncStateRepository;

    @Test
    void importsUserSessionMessagesAndUpdatesSyncState() {
        TelegramUserSyncPayload payload = new TelegramUserSyncPayload(
                List.of(
                        new TelegramUserSyncPayload.Message(
                                501L,
                                Instant.parse("2024-05-01T10:00:00Z"),
                                "Test service https://example.com"
                        )
                ),
                501L
        );

        TelegramSavedMessagesImporter.ImportResult result = importer.importPayload(payload);

        assertEquals(1, result.imported());
        assertEquals(1, postRepository.count());
        assertEquals(PostSource.USER_SESSION, postRepository.findAll().get(0).getSource());
        assertEquals(PostStatus.PENDING, postRepository.findAll().get(0).getStatus());
        assertEquals(501L, syncStateRepository.findById(1L).orElseThrow().getLastUserMessageId());
    }
}
