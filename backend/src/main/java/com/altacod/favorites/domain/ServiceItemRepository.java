package com.altacod.favorites.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {

    @Query("""
            SELECT s FROM ServiceItem s
            LEFT JOIN s.category c
            WHERE (:q IS NULL OR LOWER(s.searchText) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:categorySlug IS NULL OR c.slug = :categorySlug)
              AND (:from IS NULL OR s.postedAt >= :from)
              AND (:to IS NULL OR s.postedAt <= :to)
              AND (:hasRepo IS NULL OR (:hasRepo = TRUE AND s.repoUrl IS NOT NULL AND s.repoUrl <> ''))
            ORDER BY s.postedAt DESC, s.createdAt DESC
            """)
    Page<ServiceItem> search(
            @Param("q") String query,
            @Param("categorySlug") String categorySlug,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("hasRepo") Boolean hasRepo,
            Pageable pageable
    );

    long countByCategoryId(Long categoryId);

    @Query("""
            SELECT COUNT(s) FROM ServiceItem s
            WHERE s.repoUrl IS NOT NULL AND s.repoUrl <> ''
            """)
    long countWithRepo();
}
