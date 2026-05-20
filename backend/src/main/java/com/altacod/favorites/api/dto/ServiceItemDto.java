package com.altacod.favorites.api.dto;

import com.altacod.favorites.domain.ServiceItem;

import java.time.Instant;

public record ServiceItemDto(
        Long id,
        String title,
        String description,
        String imageUrl,
        String appUrl,
        String repoUrl,
        Integer githubStars,
        String category,
        String categorySlug,
        String tags,
        Instant postedAt,
        Instant createdAt
) {
    public static ServiceItemDto from(ServiceItem item) {
        return new ServiceItemDto(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImageUrl(),
                item.getAppUrl(),
                item.getRepoUrl(),
                item.getGithubStars(),
                item.getCategory() != null ? item.getCategory().getName() : null,
                item.getCategory() != null ? item.getCategory().getSlug() : null,
                item.getTags(),
                item.getPostedAt(),
                item.getCreatedAt()
        );
    }
}
