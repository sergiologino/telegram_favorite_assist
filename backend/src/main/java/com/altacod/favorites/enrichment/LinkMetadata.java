package com.altacod.favorites.enrichment;

public record LinkMetadata(
        String url,
        String title,
        String description,
        String imageUrl,
        String repoUrl,
        Integer githubStars
) {
    public static LinkMetadata empty(String url) {
        return new LinkMetadata(url, null, null, null, null, null);
    }
}
