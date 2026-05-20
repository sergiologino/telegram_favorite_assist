package com.altacod.favorites.processing;

import com.altacod.favorites.classification.OpenAiClassificationService;
import com.altacod.favorites.domain.*;
import com.altacod.favorites.enrichment.GitHubService;
import com.altacod.favorites.enrichment.OpenGraphService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class PostProcessingServiceTest {

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:mem:PostProcessingServiceTest");
    }

    @Autowired
    private PostProcessingService postProcessingService;

    @Autowired
    private TelegramPostRepository postRepository;

    @Autowired
    private ServiceItemRepository serviceItemRepository;

    @Test
    void processesPostWithoutOpenAi() {
        TelegramPost post = new TelegramPost();
        post.setTelegramMessageId(999L);
        post.setTextContent("OpenAI Whisper https://github.com/openai/whisper");
        post.setPostedAt(Instant.parse("2024-01-01T00:00:00Z"));
        post.setSource(PostSource.EXPORT);
        post.setStatus(PostStatus.PENDING);
        postRepository.save(post);

        PostProcessingService.ProcessingSummary summary = postProcessingService.processPendingPosts();

        assertEquals(1, summary.processed());
        assertEquals(1, serviceItemRepository.count());

        ServiceItem item = serviceItemRepository.findAll().get(0);
        assertNotNull(item.getTitle());
        assertNotNull(item.getCategory());
        assertEquals(PostStatus.DONE, postRepository.findById(post.getId()).orElseThrow().getStatus());
    }
}
