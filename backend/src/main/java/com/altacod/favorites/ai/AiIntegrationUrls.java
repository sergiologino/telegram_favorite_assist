package com.altacod.favorites.ai;

import java.util.Locale;

final class AiIntegrationUrls {

    private AiIntegrationUrls() {}

    static String normalizeBaseUrl(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.contains("://")) {
            return trimTrailingSlash(trimmed);
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("localhost") || lower.startsWith("127.0.0.1")) {
            return trimTrailingSlash("http://" + trimmed);
        }
        return trimTrailingSlash("https://" + trimmed);
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
