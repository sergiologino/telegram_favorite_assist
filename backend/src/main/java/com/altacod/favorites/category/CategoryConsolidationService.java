package com.altacod.favorites.category;

import com.altacod.favorites.config.AppProperties;
import com.altacod.favorites.domain.ServiceItem;
import com.altacod.favorites.domain.ServiceItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CategoryConsolidationService {

    private static final Logger log = LoggerFactory.getLogger(CategoryConsolidationService.class);
    private static final int PROPOSAL_SAMPLE_SIZE = 120;

    private final AppProperties properties;
    private final ServiceItemRepository serviceItemRepository;
    private final CategoryService categoryService;
    private final OpenAiCategoryService openAiCategoryService;

    public CategoryConsolidationService(
            AppProperties properties,
            ServiceItemRepository serviceItemRepository,
            CategoryService categoryService,
            OpenAiCategoryService openAiCategoryService
    ) {
        this.properties = properties;
        this.serviceItemRepository = serviceItemRepository;
        this.categoryService = categoryService;
        this.openAiCategoryService = openAiCategoryService;
    }

    public ConsolidationResult run() {
        if (!properties.categoryConsolidation().enabled()) {
            return ConsolidationResult.disabled();
        }
        if (!openAiCategoryService.isEnabled()) {
            throw new IllegalStateException("Category consolidation requires OpenAI API (OPENAI_API_KEY and OPENAI_ENABLED=true)");
        }

        long categoriesBefore = categoryService.countCategories();
        List<ServiceItem> items = serviceItemRepository.findAllWithCategory();
        if (items.isEmpty()) {
            log.info("Category consolidation skipped: catalog is empty");
            return new ConsolidationResult(categoriesBefore, categoriesBefore, 0, 0, List.of());
        }

        log.info("Category consolidation started: {} items, {} categories", items.size(), categoriesBefore);

        List<CatalogItemContext> contexts = items.stream()
                .map(this::toContext)
                .toList();

        int maxCategories = Math.max(5, properties.categoryConsolidation().maxCategories());
        int batchSize = Math.max(1, properties.categoryConsolidation().batchSize());

        List<String> canonicalCategories = openAiCategoryService.proposeCanonicalCategories(
                sampleForProposal(contexts),
                maxCategories
        );
        log.info("Proposed canonical categories ({}): {}", canonicalCategories.size(), canonicalCategories);

        Set<String> workingCategories = new LinkedHashSet<>(canonicalCategories);
        int reassigned = 0;

        for (int offset = 0; offset < contexts.size(); offset += batchSize) {
            List<CatalogItemContext> batch = contexts.subList(offset, Math.min(offset + batchSize, contexts.size()));
            Map<Long, String> assignments = openAiCategoryService.assignCategoriesBatch(
                    batch,
                    new ArrayList<>(workingCategories)
            );

            for (CatalogItemContext item : batch) {
                String categoryName = assignments.get(item.id());
                if (categoryName == null || categoryName.isBlank()) {
                    categoryName = openAiCategoryService.assignCategory(item, new ArrayList<>(workingCategories));
                }
                workingCategories.add(categoryName);
                categoryService.assignServiceItemCategory(item.id(), categoryName);
                reassigned++;
            }

            log.info("Category consolidation progress: {}/{} items", reassigned, contexts.size());
        }

        int deletedCategories = categoryService.deleteOrphanCategories();
        long categoriesAfter = categoryService.countCategories();

        if (categoriesAfter > maxCategories) {
            log.warn(
                    "Category consolidation finished with {} categories (target <= {}). Review and rerun if needed.",
                    categoriesAfter,
                    maxCategories
            );
        }

        log.info(
                "Category consolidation finished: {} -> {} categories, {} items reassigned, {} categories deleted",
                categoriesBefore,
                categoriesAfter,
                reassigned,
                deletedCategories
        );

        return new ConsolidationResult(
                categoriesBefore,
                categoriesAfter,
                reassigned,
                deletedCategories,
                categoryService.listCategoryNames()
        );
    }

    private CatalogItemContext toContext(ServiceItem item) {
        String currentCategory = item.getCategory() != null ? item.getCategory().getName() : null;
        return new CatalogItemContext(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getTags(),
                currentCategory
        );
    }

    private List<CatalogItemContext> sampleForProposal(List<CatalogItemContext> contexts) {
        if (contexts.size() <= PROPOSAL_SAMPLE_SIZE) {
            return contexts;
        }

        List<CatalogItemContext> sample = new ArrayList<>(PROPOSAL_SAMPLE_SIZE);
        double step = (double) contexts.size() / PROPOSAL_SAMPLE_SIZE;
        for (int i = 0; i < PROPOSAL_SAMPLE_SIZE; i++) {
            sample.add(contexts.get((int) Math.floor(i * step)));
        }
        return sample;
    }

    public record ConsolidationResult(
            long categoriesBefore,
            long categoriesAfter,
            int itemsReassigned,
            int categoriesDeleted,
            List<String> categories
    ) {
        public static ConsolidationResult disabled() {
            return new ConsolidationResult(0, 0, 0, 0, List.of());
        }
    }
}
