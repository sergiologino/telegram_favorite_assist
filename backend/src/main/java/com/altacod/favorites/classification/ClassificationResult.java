package com.altacod.favorites.classification;

import java.util.List;

public record ClassificationResult(
        String title,
        String description,
        String category,
        List<String> tags,
        String appUrl,
        String repoUrl
) {
}
