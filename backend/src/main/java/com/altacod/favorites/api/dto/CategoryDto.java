package com.altacod.favorites.api.dto;

public record CategoryDto(
        Long id,
        String name,
        String slug,
        long count
) {
}
