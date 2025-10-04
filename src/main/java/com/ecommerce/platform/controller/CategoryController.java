package com.ecommerce.platform.controller;

import com.ecommerce.platform.dto.CategoryRequest;
import com.ecommerce.platform.dto.CategoryResponse;
import com.ecommerce.platform.security.JwtAuthenticationFilter;
import com.ecommerce.platform.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for category management operations.
 * <p>
 * Provides CRUD operations for hierarchical categories with tenant isolation.
 * Vendors and admins can create, update, and delete categories.
 * All authenticated users can view categories.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category management endpoints")
@SecurityRequirement(name = "Bearer")
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Retrieves all categories for the current tenant.
     *
     * @param principal the authenticated user principal
     * @return list of categories
     */
    @GetMapping
    @Operation(summary = "Get all categories for tenant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categories retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @AuthenticationPrincipal JwtAuthenticationFilter.UserPrincipal principal) {
        log.info("Fetching categories for tenant: {}", principal.tenantId());
        return ResponseEntity.ok(categoryService.getCategories(principal.tenantId()));
    }

    /**
     * Retrieves a specific category by ID.
     *
     * @param principal the authenticated user principal
     * @param id        the category ID
     * @return the category details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<CategoryResponse> getCategory(
            @AuthenticationPrincipal JwtAuthenticationFilter.UserPrincipal principal,
            @PathVariable Long id) {
        log.info("Fetching category: {} for tenant: {}", id, principal.tenantId());
        return ResponseEntity.ok(categoryService.getCategory(principal.tenantId(), id));
    }

    /**
     * Creates a new category.
     * <p>
     * Requires VENDOR or ADMIN role.
     * </p>
     *
     * @param principal the authenticated user principal
     * @param request   the category creation request
     * @return the created category
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Create a new category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Category created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient role")
    })
    public ResponseEntity<CategoryResponse> createCategory(
            @AuthenticationPrincipal JwtAuthenticationFilter.UserPrincipal principal,
            @Valid @RequestBody CategoryRequest request) {
        log.info("Creating category: {} for tenant: {}", request.getName(), principal.tenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(principal.tenantId(), request));
    }

    /**
     * Updates an existing category.
     * <p>
     * Requires VENDOR or ADMIN role.
     * </p>
     *
     * @param principal the authenticated user principal
     * @param id        the category ID
     * @param request   the category update request
     * @return the updated category
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Update a category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient role"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<CategoryResponse> updateCategory(
            @AuthenticationPrincipal JwtAuthenticationFilter.UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        log.info("Updating category: {} for tenant: {}", id, principal.tenantId());
        return ResponseEntity.ok(categoryService.updateCategory(principal.tenantId(), id, request));
    }

    /**
     * Deletes a category.
     * <p>
     * Requires VENDOR or ADMIN role.
     * </p>
     *
     * @param principal the authenticated user principal
     * @param id        the category ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Delete a category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient role"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<Void> deleteCategory(
            @AuthenticationPrincipal JwtAuthenticationFilter.UserPrincipal principal,
            @PathVariable Long id) {
        log.info("Deleting category: {} for tenant: {}", id, principal.tenantId());
        categoryService.deleteCategory(principal.tenantId(), id);
        return ResponseEntity.noContent().build();
    }
}
