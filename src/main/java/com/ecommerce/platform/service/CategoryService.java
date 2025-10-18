package com.ecommerce.platform.service;

import com.ecommerce.platform.dto.CategoryRequest;
import com.ecommerce.platform.dto.CategoryResponse;
import com.ecommerce.platform.dto.PagedResponse;
import com.ecommerce.platform.entity.Category;
import com.ecommerce.platform.entity.Tenant;
import com.ecommerce.platform.exception.BadRequestException;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.platform.repository.CategoryRepository;
import com.ecommerce.platform.repository.TenantRepository;
import com.ecommerce.platform.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for category management operations.
 * <p>
 * Provides CRUD operations for hierarchical categories with tenant isolation.
 * Category lists are cached for performance.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TenantRepository tenantRepository;

    /**
     * Retrieves all categories for a tenant with pagination support.
     * <p>
     * Results are cached to improve performance.
     * </p>
     *
     * @param tenantId the tenant ID
     * @param page     the page number (0-indexed)
     * @param size     the page size
     * @return paginated response of categories
     * @throws BadRequestException if pagination parameters are invalid
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#tenantId + '_' + #page + '_' + #size")
    public PagedResponse<CategoryResponse> getCategories(Long tenantId, int page, int size) {
        log.debug("Fetching categories for tenant: {}, page: {}, size: {}", tenantId, page, size);

        // Validate pagination parameters
        if (page < 0 || size < 1) {
            throw new BadRequestException(AppConstants.ERROR_INVALID_PAGINATION);
        }

        int safeSize = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        if (page < 0) page = AppConstants.DEFAULT_PAGE;
        if (safeSize < 1) safeSize = AppConstants.DEFAULT_PAGE_SIZE;

        Pageable pageable = PageRequest.of(page, safeSize, Sort.by("name").ascending());
        Page<Category> categoryPage = categoryRepository.findByTenantId(tenantId, pageable);

        List<CategoryResponse> content = categoryPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        log.debug("Retrieved {} categories for tenant: {}", content.size(), tenantId);

        return PagedResponse.<CategoryResponse>builder()
                .content(content)
                .pageNumber(categoryPage.getNumber())
                .pageSize(categoryPage.getSize())
                .totalElements(categoryPage.getTotalElements())
                .totalPages(categoryPage.getTotalPages())
                .last(categoryPage.isLast())
                .build();
    }

    /**
     * Retrieves a specific category by ID.
     *
     * @param tenantId   the tenant ID
     * @param categoryId the category ID
     * @return the category response
     * @throws ResourceNotFoundException if category not found
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long tenantId, Long categoryId) {
        log.debug("Fetching category: {} for tenant: {}", categoryId, tenantId);
        Category category = categoryRepository.findByIdAndTenantId(categoryId, tenantId)
                .orElseThrow(() -> {
                    log.warn("Category not found: {} for tenant: {}", categoryId, tenantId);
                    return new ResourceNotFoundException("Category", categoryId);
                });
        return toResponse(category);
    }

    /**
     * Creates a new category.
     * <p>
     * Evicts the category cache for the tenant.
     * </p>
     *
     * @param tenantId the tenant ID
     * @param request  the category creation request
     * @return the created category
     * @throws ResourceNotFoundException if tenant or parent category not found
     */
    @Transactional
    @CacheEvict(value = "categories", key = "#tenantId")
    public CategoryResponse createCategory(Long tenantId, CategoryRequest request) {
        log.info("Creating category: {} for tenant: {}", request.getName(), tenantId);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> {
                    log.error("Tenant not found: {}", tenantId);
                    return new ResourceNotFoundException("Tenant", tenantId);
                });

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findByIdAndTenantId(request.getParentId(), tenantId)
                    .orElseThrow(() -> {
                        log.error("Parent category not found: {} for tenant: {}", request.getParentId(), tenantId);
                        return new ResourceNotFoundException("Parent category", request.getParentId());
                    });
        }

        Category category = Category.builder()
                .tenant(tenant)
                .name(request.getName())
                .parent(parent)
                .build();

        category = categoryRepository.save(category);
        log.info("Category created successfully: {}", category.getId());
        return toResponse(category);
    }

    /**
     * Updates an existing category.
     * <p>
     * Evicts the category cache for the tenant.
     * </p>
     *
     * @param tenantId   the tenant ID
     * @param categoryId the category ID
     * @param request    the category update request
     * @return the updated category
     * @throws ResourceNotFoundException if category or parent not found
     */
    @Transactional
    @CacheEvict(value = "categories", key = "#tenantId")
    public CategoryResponse updateCategory(Long tenantId, Long categoryId, CategoryRequest request) {
        log.info("Updating category: {} for tenant: {}", categoryId, tenantId);

        Category category = categoryRepository.findByIdAndTenantId(categoryId, tenantId)
                .orElseThrow(() -> {
                    log.warn("Category not found: {} for tenant: {}", categoryId, tenantId);
                    return new ResourceNotFoundException("Category", categoryId);
                });

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findByIdAndTenantId(request.getParentId(), tenantId)
                    .orElseThrow(() -> {
                        log.error("Parent category not found: {} for tenant: {}", request.getParentId(), tenantId);
                        return new ResourceNotFoundException("Parent category", request.getParentId());
                    });
        }

        category.setName(request.getName());
        category.setParent(parent);

        category = categoryRepository.save(category);
        log.info("Category updated successfully: {}", category.getId());
        return toResponse(category);
    }

    /**
     * Deletes a category.
     * <p>
     * Evicts the category cache for the tenant.
     * </p>
     *
     * @param tenantId   the tenant ID
     * @param categoryId the category ID
     * @throws ResourceNotFoundException if category not found
     */
    @Transactional
    @CacheEvict(value = "categories", key = "#tenantId")
    public void deleteCategory(Long tenantId, Long categoryId) {
        log.info("Deleting category: {} for tenant: {}", categoryId, tenantId);

        Category category = categoryRepository.findByIdAndTenantId(categoryId, tenantId)
                .orElseThrow(() -> {
                    log.warn("Category not found: {} for tenant: {}", categoryId, tenantId);
                    return new ResourceNotFoundException("Category", categoryId);
                });
        categoryRepository.delete(category);
        log.info("Category deleted successfully: {}", categoryId);
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .version(category.getVersion())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
