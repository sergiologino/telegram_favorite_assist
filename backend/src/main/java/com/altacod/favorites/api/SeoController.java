package com.altacod.favorites.api;

import com.altacod.favorites.domain.ServiceItem;
import com.altacod.favorites.domain.ServiceItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@RestController
public class SeoController {

    private static final DateTimeFormatter W3C = DateTimeFormatter.ISO_INSTANT;

    private final ServiceItemRepository serviceItemRepository;
    private final String siteUrl;

    public SeoController(
            ServiceItemRepository serviceItemRepository,
            @Value("${app.site-url:http://localhost:8080}") String siteUrl
    ) {
        this.serviceItemRepository = serviceItemRepository;
        this.siteUrl = trimTrailingSlash(siteUrl);
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return """
                User-agent: *
                Allow: /
                Disallow: /api/

                Sitemap: %s/sitemap.xml
                """.formatted(siteUrl);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        appendUrl(xml, "/", "daily", "1.0", Instant.now());
        appendUrl(xml, "/import", "monthly", "0.8", Instant.now());

        List<ServiceItem> services = serviceItemRepository.findAll().stream()
                .sorted(Comparator.comparing(ServiceItem::getId))
                .toList();

        for (ServiceItem service : services) {
            Instant lastModified = service.getPostedAt() != null ? service.getPostedAt() : service.getCreatedAt();
            appendUrl(xml, "/service/" + service.getId(), "weekly", "0.7", lastModified);
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private void appendUrl(StringBuilder xml, String path, String changefreq, String priority, Instant lastModified) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(siteUrl + path)).append("</loc>\n");
        if (lastModified != null) {
            xml.append("    <lastmod>")
                    .append(W3C.format(lastModified.atOffset(ZoneOffset.UTC)))
                    .append("</lastmod>\n");
        }
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8080";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
