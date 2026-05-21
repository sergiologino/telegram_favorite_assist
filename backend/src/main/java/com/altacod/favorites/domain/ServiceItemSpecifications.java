package com.altacod.favorites.domain;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ServiceItemSpecifications {

    private ServiceItemSpecifications() {
    }

    public static Specification<ServiceItem> search(
            String query,
            String categorySlug,
            Instant from,
            Instant to,
            Boolean hasRepo,
            List<String> tags
    ) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null) {
                predicates.add(cb.like(cb.lower(root.get("searchText")), "%" + query.toLowerCase(Locale.ROOT) + "%"));
            }

            if (categorySlug != null) {
                predicates.add(cb.equal(root.join("category", JoinType.LEFT).get("slug"), categorySlug));
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("postedAt"), from));
            }

            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("postedAt"), to));
            }

            if (Boolean.TRUE.equals(hasRepo)) {
                predicates.add(cb.and(
                        cb.isNotNull(root.get("repoUrl")),
                        cb.notEqual(root.get("repoUrl"), "")
                ));
            }

            if (tags != null) {
                for (String tag : tags) {
                    if (tag == null || tag.isBlank()) {
                        continue;
                    }
                    String token = tag.trim().toLowerCase(Locale.ROOT);
                    var wrappedTags = cb.concat(cb.concat(",", cb.lower(root.get("tags"))), ",");
                    var normalizedTags = cb.function("REPLACE", String.class, wrappedTags, cb.literal(" "), cb.literal(""));
                    predicates.add(cb.like(normalizedTags, "%," + token + ",%"));
                }
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
