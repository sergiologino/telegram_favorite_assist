package com.altacod.favorites.classification;

import com.altacod.favorites.category.CategoryService;
import com.altacod.favorites.config.AppProperties;
import com.altacod.favorites.enrichment.LinkMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class OpenAiClassificationService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClassificationService.class);

    private final AppProperties properties;
    private final CategoryService categoryService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiClassificationService(
            AppProperties properties,
            CategoryService categoryService,
            RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.categoryService = categoryService;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public ClassificationResult classify(String postText, List<LinkMetadata> links) {
        if (!isEnabled()) {
            return fallback(postText, links);
        }

        try {
            String prompt = buildPrompt(postText, links);
            ObjectNode request = objectMapper.createObjectNode();
            request.put("model", properties.openai().model());
            request.set("response_format", objectMapper.createObjectNode().put("type", "json_object"));
            ArrayNode messages = objectMapper.createArrayNode();
            messages.add(objectMapper.createObjectNode()
                    .put("role", "system")
                    .put("content", buildSystemPrompt()));
            messages.add(objectMapper.createObjectNode()
                    .put("role", "user")
                    .put("content", prompt));
            request.set("messages", messages);

            JsonNode response = restClient.post()
                    .uri("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + properties.openai().apiKey())
                    .header("Content-Type", "application/json")
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.has("choices") || response.get("choices").isEmpty()) {
                return fallback(postText, links);
            }

            String content = response.get("choices").get(0).get("message").get("content").asText();
            JsonNode parsed = objectMapper.readTree(content);
            return new ClassificationResult(
                    text(parsed, "title"),
                    text(parsed, "description"),
                    text(parsed, "category"),
                    readTags(parsed),
                    text(parsed, "appUrl"),
                    text(parsed, "repoUrl")
            );
        } catch (Exception ex) {
            log.warn("OpenAI classification failed, using fallback: {}", ex.getMessage());
            return fallback(postText, links);
        }
    }

    private String buildSystemPrompt() {
        List<String> existingCategories = categoryService.listCategoryNames();
        StringBuilder builder = new StringBuilder("""
                You classify saved links about software tools and services.
                Return strict JSON with keys:
                title, description, category, tags (array of strings), appUrl, repoUrl.
                Category must be a broad thematic group in Russian.
                """);
        if (existingCategories.isEmpty()) {
            builder.append("If needed, create a concise new category.\n");
            return builder.toString();
        }
        builder.append("Existing categories (prefer one of these based on item content):\n");
        for (String category : existingCategories) {
            builder.append("- ").append(category).append('\n');
        }
        builder.append("Create a new broad category only if none of the existing categories fit.\n");
        return builder.toString();
    }

    private boolean isEnabled() {
        return properties.openai().enabled()
                && properties.openai().apiKey() != null
                && !properties.openai().apiKey().isBlank();
    }

    private ClassificationResult fallback(String postText, List<LinkMetadata> links) {
        LinkMetadata primary = links.isEmpty() ? LinkMetadata.empty("") : links.get(0);
        String title = primary.title() != null ? primary.title() : firstLine(postText);
        String description = primary.description() != null ? primary.description() : postText;
        String appUrl = primary.url() != null && !primary.url().isBlank() ? primary.url() : null;
        String repoUrl = primary.repoUrl();
        String category = guessCategory(postText, primary);
        List<String> tags = List.of("imported");
        return new ClassificationResult(title, description, category, tags, appUrl, repoUrl);
    }

    private String guessCategory(String postText, LinkMetadata link) {
        String haystack = ((postText == null ? "" : postText) + " " +
                (link.description() == null ? "" : link.description()) + " " +
                (link.title() == null ? "" : link.title())).toLowerCase(Locale.ROOT);
        if (haystack.contains("transcri") || haystack.contains("транскри")) {
            return "Транскрибация";
        }
        if (haystack.contains("tts") || haystack.contains("voice") || haystack.contains("голос")) {
            return "Синтез голоса";
        }
        if (haystack.contains("video") || haystack.contains("видео")) {
            return "Работа с видео";
        }
        return "Прочее";
    }

    private String buildPrompt(String postText, List<LinkMetadata> links) {
        StringBuilder builder = new StringBuilder();
        builder.append("Telegram post text:\n").append(postText == null ? "" : postText).append("\n\nLinks metadata:\n");
        for (LinkMetadata link : links) {
            builder.append("- url: ").append(link.url()).append('\n');
            builder.append("  title: ").append(link.title()).append('\n');
            builder.append("  description: ").append(link.description()).append('\n');
            builder.append("  repo: ").append(link.repoUrl()).append('\n');
            builder.append("  stars: ").append(link.githubStars()).append('\n');
        }
        return builder.toString();
    }

    private String text(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            String value = node.get(field).asText();
            return value.isBlank() ? null : value;
        }
        return null;
    }

    private List<String> readTags(JsonNode node) {
        List<String> tags = new ArrayList<>();
        if (node.has("tags") && node.get("tags").isArray()) {
            node.get("tags").forEach(tag -> tags.add(tag.asText()));
        }
        return tags;
    }

    private String firstLine(String text) {
        if (text == null || text.isBlank()) {
            return "Без названия";
        }
        String line = text.lines().findFirst().orElse(text).trim();
        return line.length() > 120 ? line.substring(0, 120) : line;
    }
}
