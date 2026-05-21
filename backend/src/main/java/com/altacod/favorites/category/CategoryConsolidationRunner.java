package com.altacod.favorites.category;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Order(100)
@ConditionalOnProperty(name = "app.category-consolidation.enabled", havingValue = "true")
public class CategoryConsolidationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CategoryConsolidationRunner.class);

    private final CategoryConsolidationService consolidationService;

    public CategoryConsolidationRunner(CategoryConsolidationService consolidationService) {
        this.consolidationService = consolidationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Category consolidation is enabled — starting background job");
        CompletableFuture.runAsync(() -> {
            try {
                consolidationService.run();
            } catch (Exception ex) {
                log.error("Category consolidation failed: {}", ex.getMessage(), ex);
            }
        });
    }
}
