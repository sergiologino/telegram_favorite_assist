package com.altacod.favorites.category;

import com.altacod.favorites.domain.Category;
import com.altacod.favorites.domain.CategoryRepository;
import com.altacod.favorites.domain.ServiceItem;
import com.altacod.favorites.domain.ServiceItemRepository;
import com.altacod.favorites.util.SlugUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ServiceItemRepository serviceItemRepository;

    public CategoryService(CategoryRepository categoryRepository, ServiceItemRepository serviceItemRepository) {
        this.categoryRepository = categoryRepository;
        this.serviceItemRepository = serviceItemRepository;
    }

    public List<String> listCategoryNames() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(Category::getName)
                .toList();
    }

    public long countCategories() {
        return categoryRepository.count();
    }

    @Transactional
    public Category resolveCategory(String categoryName) {
        String name = (categoryName == null || categoryName.isBlank()) ? "Прочее" : categoryName.trim();
        String slug = SlugUtils.toSlug(name);
        return categoryRepository.findBySlug(slug).orElseGet(() -> {
            Category category = new Category();
            category.setName(name);
            category.setSlug(slug);
            return categoryRepository.save(category);
        });
    }

    @Transactional
    public void assignServiceItemCategory(Long itemId, String categoryName) {
        ServiceItem item = serviceItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalStateException("Service item not found: " + itemId));
        item.setCategory(resolveCategory(categoryName));
        serviceItemRepository.save(item);
    }

    @Transactional
    public int deleteOrphanCategories() {
        List<Category> categories = categoryRepository.findAll();
        int deleted = 0;
        for (Category category : categories) {
            if (serviceItemRepository.countByCategoryId(category.getId()) == 0) {
                categoryRepository.delete(category);
                deleted++;
            }
        }
        return deleted;
    }
}
