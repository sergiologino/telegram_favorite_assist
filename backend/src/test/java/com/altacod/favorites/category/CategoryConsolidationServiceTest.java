package com.altacod.favorites.category;

import com.altacod.favorites.config.AppProperties;
import com.altacod.favorites.domain.Category;
import com.altacod.favorites.domain.ServiceItem;
import com.altacod.favorites.domain.ServiceItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryConsolidationServiceTest {

    @Mock
    private ServiceItemRepository serviceItemRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private OpenAiCategoryService openAiCategoryService;

    private CategoryConsolidationService consolidationService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(
                "Europe/Moscow",
                "..",
                new AppProperties.Sync("0 0 8,20 * * *", 50),
                new AppProperties.OpenAi("key", "gpt-4o-mini", true),
                new AppProperties.AiIntegration(false, "", "", "finds-catalog", "", "admin", "", "admin@example.com"),
                new AppProperties.Telegram("", false, new AppProperties.UserApi(false, "", "", "", "python", "script", 100)),
                new AppProperties.GitHub(""),
                new AppProperties.CategoryConsolidation(true, 20, 15)
        );
        consolidationService = new CategoryConsolidationService(
                properties,
                serviceItemRepository,
                categoryService,
                openAiCategoryService
        );
    }

    @Test
    void runReassignsItemsAndDeletesOrphans() {
        Category oldCategory = new Category();
        oldCategory.setId(1L);
        oldCategory.setName("Бизнес-планирование");
        oldCategory.setSlug("бизнес-планирование");

        ServiceItem item = new ServiceItem();
        item.setId(10L);
        item.setTitle("CRM для малого бизнеса");
        item.setDescription("Сервис учёта клиентов");
        item.setTags("crm, sales");
        item.setCategory(oldCategory);

        when(openAiCategoryService.isEnabled()).thenReturn(true);
        when(categoryService.countCategories()).thenReturn(400L, 12L);
        when(serviceItemRepository.findAllWithCategory()).thenReturn(List.of(item));
        when(openAiCategoryService.proposeCanonicalCategories(anyList(), eq(20)))
                .thenReturn(List.of("Бизнес", "Разработка"));
        when(openAiCategoryService.assignCategoriesBatch(anyList(), anyList()))
                .thenReturn(Map.of(10L, "Бизнес"));
        when(categoryService.deleteOrphanCategories()).thenReturn(388);
        when(categoryService.listCategoryNames()).thenReturn(List.of("Бизнес", "Разработка"));

        CategoryConsolidationService.ConsolidationResult result = consolidationService.run();

        assertEquals(400L, result.categoriesBefore());
        assertEquals(12L, result.categoriesAfter());
        assertEquals(1, result.itemsReassigned());
        assertEquals(388, result.categoriesDeleted());
        verify(categoryService).assignServiceItemCategory(10L, "Бизнес");
    }
}
