package com.altacod.favorites.category;

import com.altacod.favorites.ai.AiIntegrationClient;
import com.altacod.favorites.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OpenAiCategoryService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCategoryService.class);

    private final AppProperties properties;
    private final AiIntegrationClient aiIntegrationClient;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiCategoryService(
            AppProperties properties,
            AiIntegrationClient aiIntegrationClient,
            RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.aiIntegrationClient = aiIntegrationClient;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return aiIntegrationClient.isEnabled() || isDirectOpenAiEnabled();
    }

    private boolean isDirectOpenAiEnabled() {
        return properties.openai().enabled()
                && properties.openai().apiKey() != null
                && !properties.openai().apiKey().isBlank();
    }

    public List<String> proposeCanonicalCategories(List<CatalogItemContext> samples, int maxCategories) {
        if (!isEnabled() || samples.isEmpty()) {
            return List.of("Прочее");
        }

        try {
            String userPrompt = buildProposalPrompt(samples, maxCategories);
            JsonNode parsed = chatJson(
                    """
                            You consolidate fragmented catalog categories into broad thematic groups.
                            Return strict JSON: {"categories": ["...", "..."]}.
                            Categories must be in Russian, broad, and suitable as top-level filters (not narrow subtopics).
                            """,
                    userPrompt
            );
            List<String> categories = readStringArray(parsed, "categories");
            return normalizeCategories(categories, maxCategories);
        } catch (Exception ex) {
            log.error("Failed to propose canonical categories: {}", ex.getMessage());
            throw new IllegalStateException("OpenAI category proposal failed: " + ex.getMessage(), ex);
        }
    }

    public Map<Long, String> assignCategoriesBatch(List<CatalogItemContext> items, List<String> canonicalCategories) {
        if (!isEnabled() || items.isEmpty()) {
            return Map.of();
        }

        try {
            String userPrompt = buildAssignmentPrompt(items, canonicalCategories);
            JsonNode parsed = chatJson(
                    """
                            You assign catalog items to broad thematic categories based on item content.
                            Return strict JSON: {"assignments":[{"id":123,"category":"..."}]}.
                            Prefer an existing category from the provided list.
                            Create a new broad category only if none of the existing categories fit the item theme.
                            Ignore the old category name when deciding — focus on title, description and tags.
                            """,
                    userPrompt
            );
            return readAssignments(parsed);
        } catch (Exception ex) {
            log.error("Failed to assign categories for batch: {}", ex.getMessage());
            throw new IllegalStateException("OpenAI category assignment failed: " + ex.getMessage(), ex);
        }
    }

    public String assignCategory(CatalogItemContext item, List<String> existingCategories) {
        Map<Long, String> assignments = assignCategoriesBatch(List.of(item), existingCategories);
        return assignments.getOrDefault(item.id(), fallbackCategory(item, existingCategories));
    }

    private JsonNode chatJson(String systemPrompt, String userPrompt) throws Exception {
        if (aiIntegrationClient.isEnabled()) {
            return aiIntegrationClient.chatJson(systemPrompt, userPrompt);
        }

        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", properties.openai().model());
        request.set("response_format", objectMapper.createObjectNode().put("type", "json_object"));
        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(objectMapper.createObjectNode().put("role", "system").put("content", systemPrompt));
        messages.add(objectMapper.createObjectNode().put("role", "user").put("content", userPrompt));
        request.set("messages", messages);

        JsonNode response = restClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + properties.openai().apiKey())
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.has("choices") || response.get("choices").isEmpty()) {
            throw new IllegalStateException("Empty OpenAI response");
        }

        String content = response.get("choices").get(0).get("message").get("content").asText();
        return objectMapper.readTree(content);
    }

    private String buildProposalPrompt(List<CatalogItemContext> samples, int maxCategories) {
        StringBuilder builder = new StringBuilder();
        builder.append("Propose up to ").append(maxCategories).append(" broad categories for this catalog.\n\n");
        builder.append("Sample items (content defines themes, not old category names):\n");
        for (CatalogItemContext item : samples) {
            appendItem(builder, item);
        }
        return builder.toString();
    }

    private String buildAssignmentPrompt(List<CatalogItemContext> items, List<String> canonicalCategories) {
        StringBuilder builder = new StringBuilder();
        builder.append("Existing categories:\n");
        for (String category : canonicalCategories) {
            builder.append("- ").append(category).append('\n');
        }
        builder.append("\nItems to classify:\n");
        for (CatalogItemContext item : items) {
            appendItem(builder, item);
        }
        return builder.toString();
    }

    private void appendItem(StringBuilder builder, CatalogItemContext item) {
        builder.append("- id: ").append(item.id()).append('\n');
        builder.append("  title: ").append(nullToEmpty(item.title())).append('\n');
        builder.append("  description: ").append(truncate(nullToEmpty(item.description()), 400)).append('\n');
        builder.append("  tags: ").append(nullToEmpty(item.tags())).append('\n');
        builder.append("  oldCategory: ").append(nullToEmpty(item.currentCategory())).append(" (ignore for decision)\n");
    }

    private Map<Long, String> readAssignments(JsonNode parsed) {
        Map<Long, String> assignments = new LinkedHashMap<>();
        if (!parsed.has("assignments") || !parsed.get("assignments").isArray()) {
            return assignments;
        }
        for (JsonNode node : parsed.get("assignments")) {
            if (!node.has("id")) {
                continue;
            }
            long id = node.get("id").asLong();
            String category = node.path("category").asText("").trim();
            if (!category.isBlank()) {
                assignments.put(id, category);
            }
        }
        return assignments;
    }

    private List<String> readStringArray(JsonNode parsed, String field) {
        List<String> values = new ArrayList<>();
        if (parsed.has(field) && parsed.get(field).isArray()) {
            parsed.get(field).forEach(node -> {
                String value = node.asText("").trim();
                if (!value.isBlank()) {
                    values.add(value);
                }
            });
        }
        return values;
    }

    private List<String> normalizeCategories(List<String> categories, int maxCategories) {
        List<String> normalized = categories.stream()
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .distinct()
                .limit(maxCategories)
                .collect(Collectors.toCollection(ArrayList::new));
        if (normalized.isEmpty()) {
            normalized.add("Прочее");
        }
        return normalized;
    }

    private String fallbackCategory(CatalogItemContext item, List<String> existingCategories) {
        String haystack = (nullToEmpty(item.title()) + " " + nullToEmpty(item.description()) + " " + nullToEmpty(item.tags()))
                .toLowerCase(Locale.ROOT);
        for (String category : existingCategories) {
            String token = category.toLowerCase(Locale.ROOT);
            if (haystack.contains(token)) {
                return category;
            }
        }
        return existingCategories.isEmpty() ? "Прочее" : existingCategories.get(0);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
