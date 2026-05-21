package com.altacod.favorites.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ServiceItemSpecificationsTest {

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:mem:ServiceItemSpecificationsTest");
    }

    @Autowired
    private ServiceItemRepository serviceItemRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void filtersByTagsAndPaginates() {
        Category category = new Category();
        category.setName("Tools");
        category.setSlug("tools");
        categoryRepository.save(category);

        ServiceItem first = new ServiceItem();
        first.setTitle("First");
        first.setTags("ai, voice");
        first.setCategory(category);
        first.setSearchText("first");
        first.setPostedAt(Instant.parse("2024-02-01T00:00:00Z"));

        ServiceItem second = new ServiceItem();
        second.setTitle("Second");
        second.setTags("ai, video");
        second.setCategory(category);
        second.setSearchText("second");
        second.setPostedAt(Instant.parse("2024-01-01T00:00:00Z"));

        serviceItemRepository.saveAll(List.of(first, second));

        Specification<ServiceItem> specification = ServiceItemSpecifications.search(
                null,
                null,
                null,
                null,
                null,
                List.of("voice")
        );

        Page<ServiceItem> page = serviceItemRepository.findAll(
                specification,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "postedAt", "createdAt"))
        );

        assertEquals(1, page.getTotalElements());
        assertEquals("First", page.getContent().get(0).getTitle());
    }
}
