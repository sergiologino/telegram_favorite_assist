package com.altacod.favorites.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UrlExtractor {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
            Pattern.CASE_INSENSITIVE
    );

    private UrlExtractor() {
    }

    public static List<String> extractUrls(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Set<String> urls = new LinkedHashSet<>();
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            String url = trimTrailingPunctuation(matcher.group(1));
            if (!url.isBlank()) {
                urls.add(url);
            }
        }
        return new ArrayList<>(urls);
    }

    private static String trimTrailingPunctuation(String url) {
        while (!url.isEmpty() && ",.;)]}".indexOf(url.charAt(url.length() - 1)) >= 0) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
