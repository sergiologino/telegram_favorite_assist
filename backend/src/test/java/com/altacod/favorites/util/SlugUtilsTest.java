package com.altacod.favorites.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlugUtilsTest {

    @Test
    void createsSlugFromCyrillic() {
        assertEquals("транскрибация", SlugUtils.toSlug("Транскрибация"));
    }

    @Test
    void returnsOtherForBlank() {
        assertEquals("other", SlugUtils.toSlug("   "));
    }
}
