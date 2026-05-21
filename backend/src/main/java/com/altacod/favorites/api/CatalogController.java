package com.altacod.favorites.api;

import com.altacod.favorites.api.dto.CategoryDto;
import com.altacod.favorites.api.dto.PageDto;
import com.altacod.favorites.api.dto.ServiceItemDto;
import com.altacod.favorites.api.dto.StatsDto;
import com.altacod.favorites.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
            @RequestParam(required = false) String tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Instant fromInstant = from != null ? from.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusMillis(1) : null;
        List<String> tagList = parseTagFilter(tags);

        Specification<ServiceItem> specification = ServiceItemSpecifications.search(
                blankToNull(q),
                blankToNull(category),
                fromInstant,
                toInstant,
                hasRepo,
                tagList
        );

        Page<ServiceItem> result = serviceItemRepository.findAll(
                specification,
                PageRequest.of(page, Math.max(1, size), Sort.by(Sort.Direction.DESC, "postedAt", "createdAt"))
        );

        List<ServiceItemDto> items = result.getContent().stream().map(ServiceItemDto::from).toList();
        return new PageDto<>(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/tags")
    public List<String> listTags(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Boolean hasRepo
    ) {
        Instant fromInstant = from != null ? from.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusMillis(1) : null;

        Specification<ServiceItem> specification = ServiceItemSpecifications.search(
                blankToNull(q),
                blankToNull(category),
                fromInstant,
                toInstant,
                hasRepo,
                List.of()
        );

        Set<String> uniqueTags = new LinkedHashSet<>();
        for (ServiceItem item : serviceItemRepository.findAll(specification)) {
            for (String tag : splitStoredTags(item.getTags())) {
                uniqueTags.add(tag);
            }
        }

        return uniqueTags.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
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

    private List<String> parseTagFilter(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .toList();
    }

    private List<String> splitStoredTags(String tagsField) {
        if (tagsField == null || tagsField.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tagsField.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
    }
}
