package com.altacod.favorites.telegram;

import com.altacod.favorites.domain.PostSource;
import com.altacod.favorites.domain.PostStatus;
import com.altacod.favorites.domain.TelegramPost;
import com.altacod.favorites.domain.TelegramPostRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TelegramExportParser {

    private static final Logger log = LoggerFactory.getLogger(TelegramExportParser.class);
    private static final DateTimeFormatter EXPORT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final TelegramPostRepository postRepository;

    public TelegramExportParser(TelegramPostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public ImportResult importExport(JsonNode root) {
        JsonNode messages = root.path("messages");
        if (!messages.isArray()) {
            throw new IllegalArgumentException("Invalid Telegram export: messages array not found");
        }

        int imported = 0;
        int skippedDuplicate = 0;
        int skippedEmpty = 0;

        for (JsonNode message : messages) {
            if (!"message".equals(message.path("type").asText())) {
                continue;
            }
            long messageId = message.path("id").asLong();
            if (postRepository.findByTelegramMessageId(messageId).isPresent()) {
                skippedDuplicate++;
                continue;
            }

            String content = extractImportableContent(message);
            if (content.isBlank()) {
                skippedEmpty++;
                continue;
            }

            TelegramPost post = new TelegramPost();
            post.setTelegramMessageId(messageId);
            post.setTextContent(content);
            post.setPostedAt(parseDate(message));
            post.setSource(PostSource.EXPORT);
            post.setStatus(PostStatus.PENDING);
            postRepository.save(post);
            imported++;
        }

        log.info(
                "Telegram export import finished: imported={}, skippedDuplicate={}, skippedEmpty={}",
                imported,
                skippedDuplicate,
                skippedEmpty
        );
        return new ImportResult(imported, skippedDuplicate, skippedEmpty);
    }

    String extractImportableContent(JsonNode message) {
        Set<String> parts = new LinkedHashSet<>();

        addIfPresent(parts, extractText(message.path("text")));
        collectEntityParts(message.path("text"), parts);
        collectEntityParts(message.path("text_entities"), parts);
        collectWebPageParts(message, parts);

        return String.join("\n", parts).trim();
    }

    private void collectWebPageParts(JsonNode message, Set<String> parts) {
        String mediaType = message.path("media_type").asText("");
        if (!"web_page".equalsIgnoreCase(mediaType)) {
            return;
        }

        addIfPresent(parts, message.path("title").asText(null));
        addIfPresent(parts, message.path("description").asText(null));
        addIfPresent(parts, message.path("site_name").asText(null));
        addIfPresent(parts, firstNonBlank(
                message.path("href").asText(null),
                message.path("url").asText(null),
                message.path("link").asText(null)
        ));
    }

    private void collectEntityParts(JsonNode entitiesNode, Set<String> parts) {
        if (!entitiesNode.isArray()) {
            return;
        }

        for (JsonNode entity : entitiesNode) {
            if (!entity.isObject()) {
                continue;
            }

            String type = entity.path("type").asText("");
            String text = entity.path("text").asText("").trim();
            String href = entity.path("href").asText("").trim();

            if ("text_link".equals(type)) {
                if (!text.isBlank()) {
                    addIfPresent(parts, text);
                }
                addIfPresent(parts, href);
                continue;
            }

            if ("link".equals(type) && !text.isBlank()) {
                addIfPresent(parts, normalizeLink(text));
                continue;
            }

            if ("url".equals(type) && !text.isBlank()) {
                addIfPresent(parts, text);
            }
        }
    }

    String extractText(JsonNode textNode) {
        if (textNode.isNull() || textNode.isMissingNode()) {
            return "";
        }
        if (textNode.isTextual()) {
            return textNode.asText();
        }
        if (!textNode.isArray()) {
            return textNode.asText("");
        }

        StringBuilder builder = new StringBuilder();
        for (JsonNode part : textNode) {
            if (part.isTextual()) {
                builder.append(part.asText());
            } else if (part.isObject()) {
                builder.append(part.path("text").asText(""));
            }
        }
        return builder.toString().trim();
    }

    private String normalizeLink(String value) {
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return "";
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    private void addIfPresent(Set<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.trim());
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private Instant parseDate(JsonNode message) {
        String value = message.path("date").asText(null);
        if (value != null && !value.isBlank()) {
            try {
                if (value.endsWith("Z") || value.contains("+") || value.contains("-") && value.indexOf('-', 10) > 0) {
                    return Instant.parse(value);
                }
                LocalDateTime localDateTime = LocalDateTime.parse(value, EXPORT_FORMAT);
                return localDateTime.toInstant(ZoneOffset.UTC);
            } catch (Exception ex) {
                log.warn("Unable to parse export date '{}', trying date_unixtime", value);
            }
        }

        String unixtime = message.path("date_unixtime").asText(null);
        if (unixtime != null && !unixtime.isBlank()) {
            try {
                return Instant.ofEpochSecond(Long.parseLong(unixtime));
            } catch (NumberFormatException ex) {
                log.warn("Unable to parse date_unixtime '{}'", unixtime);
            }
        }

        return Instant.now();
    }

    public record ImportResult(int imported, int skippedDuplicate, int skippedEmpty) {
        public int skipped() {
            return skippedDuplicate + skippedEmpty;
        }
    }
}
