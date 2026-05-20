package com.altacod.favorites.api;

import com.altacod.favorites.api.dto.CategoryDto;
import com.altacod.favorites.api.dto.PageDto;
import com.altacod.favorites.api.dto.ServiceItemDto;
import com.altacod.favorites.api.dto.StatsDto;
import com.altacod.favorites.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final ServiceItemRepository serviceItemRepository;
    private final CategoryRepository categoryRepository;
    private final TelegramPostRepository postRepository;

    public CatalogController(
            ServiceItemRepository serviceItemRepository,
            CategoryRepository categoryRepository,
            TelegramPostRepository postRepository
    ) {
        this.serviceItemRepository = serviceItemRepository;
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    @GetMapping("/services")
    public PageDto<ServiceItemDto> listServices(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Boolean hasRepo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size
    ) {
        Instant fromInstant = from != null ? from.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusMillis(1) : null;

        Page<ServiceItem> result = serviceItemRepository.search(
                blankToNull(q),
                blankToNull(category),
                fromInstant,
                toInstant,
                hasRepo,
                PageRequest.of(page, size)
        );

        List<ServiceItemDto> items = result.getContent().stream().map(ServiceItemDto::from).toList();
        return new PageDto<>(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/services/{id}")
    public ServiceItemDto getService(@PathVariable Long id) {
        ServiceItem item = serviceItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service not found"));
        return ServiceItemDto.from(item);
    }

    @GetMapping("/categories")
    public List<CategoryDto> listCategories() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getName))
                .map(category -> new CategoryDto(
                        category.getId(),
                        category.getName(),
                        category.getSlug(),
                        serviceItemRepository.countByCategoryId(category.getId())
                ))
                .toList();
    }

    @GetMapping("/stats")
    public StatsDto stats() {
        return new StatsDto(
                serviceItemRepository.count(),
                categoryRepository.count(),
                postRepository.countByStatus(PostStatus.PENDING),
                postRepository.countByStatus(PostStatus.FAILED),
                serviceItemRepository.countWithRepo()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
