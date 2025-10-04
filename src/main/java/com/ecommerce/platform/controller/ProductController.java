package com.ecommerce.platform.controller;

import com.ecommerce.platform.dto.PagedResponse;
import com.ecommerce.platform.dto.ProductRequest;
import com.ecommerce.platform.dto.ProductResponse;
import com.ecommerce.platform.security.JwtAuthenticationFilter;
import com.ecommerce.platform.service.ProductService;
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

/**
 * REST controller for product management operations.
 * <p>
 * Provides CRUD operations for products with tenant isolation.
 * Vendors and admins can create, update, and delete products.
 * All authenticated users can view products.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management endpoints")
@SecurityRequirement(name = "Bearer")
@Slf4j
public class ProductController {

    private final ProductService productService;

    /**
     * Retrieves all products for the current tenant with pagination support.
     *
     * @param principal the authenticated user principal
     * @param page      the page number (0-indexed)
     * @param size      the page size
     * @param categoryId optional category filter
     * @return paginated list of products
     */
    @GetMapping
    @Operation(summary = "Get all products for tenant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PagedResponse<ProductResponse>> getProducts(
            @AuthenticationPrincipal JwtAuthenticationFilter.UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId) {
        log.info("Fetching products for tenant: {}, page: {}, size: {}", principal.tenantId(), page, size);
        return ResponseEntity.ok(productService.getProducts(principal.tenantId(), page, size, categoryId));
    }

    /**
     * Retrieves a specific product by ID.
     *
     * @param principal the authenticated user principal
     * @param id        the product ID
     * @return the product details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getProduct(
            @AuthenticationPrincipal JwtAuthenticationFilter.UserPrincipal principal,
            @PathVariable Long id) {
        log.info("Fetching product: {} for tenant: {}", id, principal.tenantId());
        return ResponseEntity.ok(productService.getProduct(principal.tenantId(), id));
    }

    /**
     * Creates a new product.
     * <p>
     * Requires VENDOR or ADMIN role.
     * </p>
     *
     * @param principal the authenticated user principal
     * @param request   the product creation request
     * @return the created product
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Create a new product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient role")
    })
    public ResponseEntity<ProductResponse> createProduct(
            @AuthenticationPrincipal JwtAuthenticationFilter.UserPrincipal principal,
            @Valid @RequestBody ProductRequest request) {
        log.info("Creating product: {} for tenant: {}", request.getName(), principal.tenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(principal.tenantId(), request));
    }

    /**
     * Updates an existing product.
     * <p>
     * Requires VENDOR or ADMIN role.
     * </p>
     *
     * @param principal the authenticated user principal
     * @param id        the product ID
     * @param request   the product update request
     * @return the updated product
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Update a product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient role"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> updateProduct(
            @AuthenticationPrincipal JwtAuthenticationFilter.UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        log.info("Updating product: {} for tenant: {}", id, principal.tenantId());
        return ResponseEntity.ok(productService.updateProduct(principal.tenantId(), id, request));
    }

    /**
     * Deletes a product.
     * <p>
     * Requires VENDOR or ADMIN role.
     * </p>
     *
     * @param principal the authenticated user principal
     * @param id        the product ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Delete a product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient role"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Void> deleteProduct(
            @AuthenticationPrincipal JwtAuthenticationFilter.UserPrincipal principal,
            @PathVariable Long id) {
        log.info("Deleting product: {} for tenant: {}", id, principal.tenantId());
        productService.deleteProduct(principal.tenantId(), id);
        return ResponseEntity.noContent().build();
    }
}
