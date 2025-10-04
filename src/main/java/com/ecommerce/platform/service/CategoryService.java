package com.ecommerce.platform.service;

import com.ecommerce.platform.dto.CategoryRequest;
import com.ecommerce.platform.dto.CategoryResponse;
import com.ecommerce.platform.entity.Category;
import com.ecommerce.platform.entity.Tenant;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.platform.repository.CategoryRepository;
import com.ecommerce.platform.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
     * Retrieves all categories for a tenant.
     * <p>
     * Results are cached to improve performance.
     * </p>
     *
     * @param tenantId the tenant ID
     * @return list of categories
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#tenantId")
    public List<CategoryResponse> getCategories(Long tenantId) {
        log.debug("Fetching categories for tenant: {}", tenantId);
        List<CategoryResponse> categories = categoryRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        log.debug("Retrieved {} categories for tenant: {}", categories.size(), tenantId);
        return categories;
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
