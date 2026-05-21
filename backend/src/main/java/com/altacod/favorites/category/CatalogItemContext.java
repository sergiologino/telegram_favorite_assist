package com.altacod.favorites.category;

public record CatalogItemContext(
        Long id,
        String title,
        String description,
        String tags,
        String currentCategory
) {
}
