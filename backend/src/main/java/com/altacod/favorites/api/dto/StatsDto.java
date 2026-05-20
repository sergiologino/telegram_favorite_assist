package com.altacod.favorites.api.dto;

public record StatsDto(
        long totalServices,
        long totalCategories,
        long pendingPosts,
        long failedPosts,
        long githubServices
) {
}
