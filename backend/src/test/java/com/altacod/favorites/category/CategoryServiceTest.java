package com.altacod.favorites.category;

import com.altacod.favorites.domain.Category;
import com.altacod.favorites.domain.CategoryRepository;
import com.altacod.favorites.domain.ServiceItem;
import com.altacod.favorites.domain.ServiceItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(CategoryService.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategoryServiceTest {

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:mem:CategoryServiceTest");
    }

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ServiceItemRepository serviceItemRepository;

    @Test
    void resolveCategoryReusesExistingSlug() {
        Category first = categoryService.resolveCategory("Бизнес");
        Category second = categoryService.resolveCategory("Бизнес");

        assertEquals(first.getId(), second.getId());
        assertEquals(1, categoryRepository.count());
    }

    @Test
    void deleteOrphanCategoriesRemovesUnusedOnly() {
        Category used = categoryService.resolveCategory("Разработка");
        Category unused = categoryService.resolveCategory("Временная");

        ServiceItem item = new ServiceItem();
        item.setTitle("Test");
        item.setCategory(used);
        serviceItemRepository.save(item);

        int deleted = categoryService.deleteOrphanCategories();

        assertEquals(1, deleted);
        assertEquals(1, categoryRepository.count());
        assertTrue(categoryRepository.findById(used.getId()).isPresent());
        assertFalse(categoryRepository.findById(unused.getId()).isPresent());
    }
}
