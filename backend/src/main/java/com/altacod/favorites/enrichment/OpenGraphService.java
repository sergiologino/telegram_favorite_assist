package com.altacod.favorites.enrichment;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OpenGraphService {

    private static final Logger log = LoggerFactory.getLogger(OpenGraphService.class);
    private static final Pattern GITHUB_REPO = Pattern.compile(
            "https?://(?:www\\.)?github\\.com/([^/]+)/([^/?#]+)/?",
            Pattern.CASE_INSENSITIVE
    );

    public LinkMetadata enrich(String url, GitHubService gitHubService) {
        OpenGraphService.LinkMetadataBuilder builder = OpenGraphService.LinkMetadataBuilder.from(url);

        try {
            Document document = Jsoup.connect(url)
                    .userAgent("TelegramFavoritesAssist/1.0")
                    .timeout(10_000)
                    .followRedirects(true)
                    .get();

            builder.title(firstNonBlank(
                    meta(document, "og:title"),
                    meta(document, "twitter:title"),
                    document.title()
            ));
            builder.description(firstNonBlank(
                    meta(document, "og:description"),
                    meta(document, "twitter:description"),
                    meta(document, "description")
            ));
            builder.imageUrl(firstNonBlank(
                    meta(document, "og:image"),
                    meta(document, "twitter:image")
            ));
        } catch (Exception ex) {
            log.warn("Failed to fetch Open Graph for {}: {}", url, ex.getMessage());
        }

        String repoUrl = extractGitHubRepo(url).orElse(null);
        if (repoUrl == null && builder.title != null) {
            repoUrl = extractGitHubRepo(builder.title).orElse(null);
        }
        builder.repoUrl(repoUrl);

        if (repoUrl != null) {
            builder.githubStars(gitHubService.fetchStars(repoUrl).orElse(null));
        }

        return builder.build();
    }

    public Optional<String> extractGitHubRepo(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = GITHUB_REPO.matcher(value.trim());
        if (!matcher.find()) {
            return Optional.empty();
        }
        String owner = matcher.group(1);
        String repo = matcher.group(2).replaceAll("\\.git$", "");
        return Optional.of("https://github.com/" + owner + "/" + repo);
    }

    private static String meta(Document document, String property) {
        String content = document.select("meta[property=" + property + "]").attr("content");
        if (content.isBlank()) {
            content = document.select("meta[name=" + property + "]").attr("content");
        }
        return content.isBlank() ? null : content.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    static final class LinkMetadataBuilder {
        private final String url;
        private String title;
        private String description;
        private String imageUrl;
        private String repoUrl;
        private Integer githubStars;

        private LinkMetadataBuilder(String url) {
            this.url = url;
        }

        static LinkMetadataBuilder from(String url) {
            LinkMetadataBuilder builder = new LinkMetadataBuilder(url);
            try {
                URI uri = URI.create(url);
                builder.title = uri.getHost();
            } catch (Exception ignored) {
                builder.title = url;
            }
            return builder;
        }

        LinkMetadataBuilder title(String title) {
            if (title != null && !title.isBlank()) {
                this.title = title;
            }
            return this;
        }

        LinkMetadataBuilder description(String description) {
            this.description = description;
            return this;
        }

        LinkMetadataBuilder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        LinkMetadataBuilder repoUrl(String repoUrl) {
            this.repoUrl = repoUrl;
            return this;
        }

        LinkMetadataBuilder githubStars(Integer githubStars) {
            this.githubStars = githubStars;
            return this;
        }

        LinkMetadata build() {
            return new LinkMetadata(url, title, description, imageUrl, repoUrl, githubStars);
        }
    }
}
