package com.altacod.favorites.processing;

import com.altacod.favorites.classification.ClassificationResult;
import com.altacod.favorites.classification.OpenAiClassificationService;
import com.altacod.favorites.category.CategoryService;
import com.altacod.favorites.config.AppProperties;
import com.altacod.favorites.domain.*;
import com.altacod.favorites.enrichment.GitHubService;
import com.altacod.favorites.enrichment.LinkMetadata;
import com.altacod.favorites.enrichment.OpenGraphService;
import com.altacod.favorites.util.UrlExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class PostProcessingService {

    private static final Logger log = LoggerFactory.getLogger(PostProcessingService.class);

    private final TelegramPostRepository postRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final OpenGraphService openGraphService;
    private final GitHubService gitHubService;
    private final OpenAiClassificationService classificationService;
    private final CategoryService categoryService;
    private final AppProperties properties;

    public PostProcessingService(
            TelegramPostRepository postRepository,
            ServiceItemRepository serviceItemRepository,
            OpenGraphService openGraphService,
            GitHubService gitHubService,
            OpenAiClassificationService classificationService,
            CategoryService categoryService,
            AppProperties properties
    ) {
        this.postRepository = postRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.openGraphService = openGraphService;
        this.gitHubService = gitHubService;
        this.classificationService = classificationService;
        this.categoryService = categoryService;
        this.properties = properties;
    }

    @Transactional
    public ProcessingSummary processPendingPosts() {
        int batchSize = Math.max(1, properties.sync().processBatchSize());
        List<TelegramPost> pending = postRepository.findByStatusOrderByPostedAtDesc(
                PostStatus.PENDING,
                PageRequest.of(0, batchSize)
        );
        int processed = 0;
        int failed = 0;
        int skipped = 0;

        for (TelegramPost post : pending) {
            try {
                PostStatus status = processPost(post);
                if (status == PostStatus.DONE) {
                    processed++;
                } else if (status == PostStatus.SKIPPED) {
                    skipped++;
                }
            } catch (Exception ex) {
                post.setStatus(PostStatus.FAILED);
                post.setErrorMessage(ex.getMessage());
                postRepository.save(post);
                failed++;
                log.error("Failed to process post {}: {}", post.getId(), ex.getMessage());
            }
        }

        return new ProcessingSummary(processed, failed, skipped);
    }

    @Transactional
    public PostStatus processPost(TelegramPost post) {
        post.setStatus(PostStatus.PROCESSING);
        postRepository.save(post);

        List<String> urls = UrlExtractor.extractUrls(post.getTextContent());
        if (urls.isEmpty() && (post.getTextContent() == null || post.getTextContent().isBlank())) {
            post.setStatus(PostStatus.SKIPPED);
            postRepository.save(post);
            return PostStatus.SKIPPED;
        }

        List<LinkMetadata> enrichedLinks = new ArrayList<>();
        for (String url : urls) {
            enrichedLinks.add(openGraphService.enrich(url, gitHubService));
        }

        ClassificationResult classification = classificationService.classify(post.getTextContent(), enrichedLinks);
        LinkMetadata primaryLink = enrichedLinks.isEmpty() ? LinkMetadata.empty("") : enrichedLinks.get(0);

        ServiceItem item = new ServiceItem();
        item.setTitle(firstNonBlank(classification.title(), primaryLink.title(), "Без названия"));
        item.setDescription(firstNonBlank(classification.description(), post.getTextContent()));
        item.setImageUrl(primaryLink.imageUrl());
        item.setAppUrl(firstNonBlank(classification.appUrl(), primaryLink.url()));
        item.setRepoUrl(firstNonBlank(classification.repoUrl(), primaryLink.repoUrl()));
        item.setGithubStars(primaryLink.githubStars());
        item.setCategory(categoryService.resolveCategory(classification.category()));
        item.setTags(classification.tags().stream().collect(Collectors.joining(", ")));
        item.setPostedAt(post.getPostedAt());
        item.setTelegramPost(post);
        item.setSearchText(buildSearchText(item, post.getTextContent(), classification.tags()));
        serviceItemRepository.save(item);

        post.setStatus(PostStatus.DONE);
        post.setErrorMessage(null);
        postRepository.save(post);
        return PostStatus.DONE;
    }

    private String buildSearchText(ServiceItem item, String postText, List<String> tags) {
        return String.join(" ",
                safe(item.getTitle()),
                safe(item.getDescription()),
                safe(postText),
                safe(item.getAppUrl()),
                safe(item.getRepoUrl()),
                tags.stream().collect(Collectors.joining(" "))
        ).toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public record ProcessingSummary(int processed, int failed, int skipped) {
    }
}
