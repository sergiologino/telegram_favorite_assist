package com.altacod.favorites.enrichment;

import com.altacod.favorites.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private static final Pattern GITHUB_REPO = Pattern.compile(
            "github\\.com/([^/]+)/([^/?#]+)",
            Pattern.CASE_INSENSITIVE
    );

    private final RestClient restClient;
    private final AppProperties properties;

    public GitHubService(RestClient restClient, AppProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public Optional<Integer> fetchStars(String repoUrl) {
        Optional<String[]> parts = parseRepo(repoUrl);
        if (parts.isEmpty()) {
            return Optional.empty();
        }
        String owner = parts.get()[0];
        String repo = parts.get()[1];

        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                    .uri("https://api.github.com/repos/{owner}/{repo}", owner, repo);
            if (properties.github().token() != null && !properties.github().token().isBlank()) {
                request = request.header("Authorization", "Bearer " + properties.github().token());
            }
            JsonNode response = request.retrieve().body(JsonNode.class);
            if (response != null && response.has("stargazers_count")) {
                return Optional.of(response.get("stargazers_count").asInt());
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch GitHub stars for {}: {}", repoUrl, ex.getMessage());
        }
        return Optional.empty();
    }

    Optional<String[]> parseRepo(String repoUrl) {
        if (repoUrl == null || repoUrl.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = GITHUB_REPO.matcher(repoUrl);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new String[]{
                matcher.group(1),
                matcher.group(2).replaceAll("\\.git$", "")
        });
    }
}
