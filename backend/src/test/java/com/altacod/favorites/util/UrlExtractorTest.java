package com.altacod.favorites.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UrlExtractorTest {

    @Test
    void extractsMultipleUrls() {
        List<String> urls = UrlExtractor.extractUrls(
                "Check https://example.com and https://github.com/openai/whisper."
        );
        assertEquals(2, urls.size());
        assertEquals("https://example.com", urls.get(0));
        assertEquals("https://github.com/openai/whisper", urls.get(1));
    }

    @Test
    void returnsEmptyForBlankText() {
        assertTrue(UrlExtractor.extractUrls("  ").isEmpty());
    }
}
