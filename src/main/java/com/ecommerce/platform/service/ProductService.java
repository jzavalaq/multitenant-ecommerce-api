package com.ecommerce.platform.service;

import com.ecommerce.platform.dto.PagedResponse;
import com.ecommerce.platform.dto.ProductRequest;
import com.ecommerce.platform.dto.ProductResponse;
import com.ecommerce.platform.entity.Category;
import com.ecommerce.platform.entity.Product;
import com.ecommerce.platform.entity.ProductVariant;
import com.ecommerce.platform.entity.Tenant;
import com.ecommerce.platform.exception.BadRequestException;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.platform.repository.CategoryRepository;
import com.ecommerce.platform.repository.ProductRepository;
import com.ecommerce.platform.repository.ProductVariantRepository;
import com.ecommerce.platform.repository.TenantRepository;
import com.ecommerce.platform.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for product management operations.
 * <p>
 * Provides CRUD operations for products with tenant isolation.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final TenantRepository tenantRepository;

    /**
     * Retrieves all products for a tenant with pagination support.
     * <p>
     * Uses batch loading to avoid N+1 queries when fetching variants.
     * </p>
     *
     * @param tenantId   the tenant ID
     * @param page       the page number (0-indexed)
     * @param size       the page size
     * @param categoryId optional category filter
     * @return paginated response of products
     * @throws BadRequestException if pagination parameters are invalid
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProducts(Long tenantId, int page, int size, Long categoryId) {
        log.debug("Fetching products for tenant: {}, page: {}, size: {}, category: {}", tenantId, page, size, categoryId);

        // Validate pagination parameters
        if (page < 0 || size < 1) {
            throw new BadRequestException(AppConstants.ERROR_INVALID_PAGINATION);
        }

        int safeSize = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        if (page < 0) page = AppConstants.DEFAULT_PAGE;
        if (safeSize < 1) safeSize = AppConstants.DEFAULT_PAGE_SIZE;

        Pageable pageable = PageRequest.of(page, safeSize, Sort.by("createdAt").descending());

        // Fetch product IDs with pagination to avoid N+1 queries
        Page<Long> productIdPage;
        if (categoryId != null) {
            productIdPage = productRepository.findIdsByTenantIdAndCategoryId(tenantId, categoryId, pageable);
        } else {
            productIdPage = productRepository.findIdsByTenantId(tenantId, pageable);
        }

        if (productIdPage.isEmpty()) {
            return PagedResponse.<ProductResponse>builder()
                    .content(List.of())
                    .pageNumber(productIdPage.getNumber())
                    .pageSize(productIdPage.getSize())
                    .totalElements(productIdPage.getTotalElements())
                    .totalPages(productIdPage.getTotalPages())
                    .last(productIdPage.isLast())
                    .build();
        }

        // Batch load products with categories
        List<Product> products = productRepository.findByIdsWithCategory(productIdPage.getContent());

        // Batch load all variants for the products in a single query
        List<ProductVariant> allVariants = variantRepository.findByProductIdIn(productIdPage.getContent());

        // Group variants by product ID for efficient lookup
        Map<Long, List<ProductVariant>> variantsByProductId = allVariants.stream()
                .collect(Collectors.groupingBy(v -> v.getProduct().getId()));

        // Map products to responses using pre-loaded variants
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<ProductResponse> content = productIdPage.getContent().stream()
                .map(productMap::get)
                .map(product -> toResponse(product, variantsByProductId.getOrDefault(product.getId(), List.of())))
                .collect(Collectors.toList());

        log.debug("Retrieved {} products for tenant: {}", content.size(), tenantId);

        return PagedResponse.<ProductResponse>builder()
                .content(content)
                .pageNumber(productIdPage.getNumber())
                .pageSize(productIdPage.getSize())
                .totalElements(productIdPage.getTotalElements())
                .totalPages(productIdPage.getTotalPages())
                .last(productIdPage.isLast())
                .build();
    }

    /**
     * Retrieves a specific product by ID.
     *
     * @param tenantId  the tenant ID
     * @param productId the product ID
     * @return the product response
     * @throws ResourceNotFoundException if product not found
     */
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long tenantId, Long productId) {
        log.debug("Fetching product: {} for tenant: {}", productId, tenantId);
        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> {
                    log.warn("Product not found: {} for tenant: {}", productId, tenantId);
                    return new ResourceNotFoundException("Product", productId);
                });
        return toResponse(product);
    }

    /**
     * Creates a new product with variants.
     *
     * @param tenantId the tenant ID
     * @param request  the product creation request
     * @return the created product
     * @throws ResourceNotFoundException if tenant or category not found
     */
    @Transactional
    public ProductResponse createProduct(Long tenantId, ProductRequest request) {
        log.info("Creating product: {} for tenant: {}", request.getName(), tenantId);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> {
                    log.error("Tenant not found: {}", tenantId);
                    return new ResourceNotFoundException("Tenant", tenantId);
                });

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findByIdAndTenantId(request.getCategoryId(), tenantId)
                    .orElseThrow(() -> {
                        log.error("Category not found: {} for tenant: {}", request.getCategoryId(), tenantId);
                        return new ResourceNotFoundException("Category", request.getCategoryId());
                    });
        }

        Product product = Product.builder()
                .tenant(tenant)
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .build();

        product = productRepository.save(product);

        for (ProductRequest.VariantRequest vr : request.getVariants()) {
            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .sku(vr.getSku())
                    .price(vr.getPrice())
                    .stock(vr.getStock())
                    .build();
            variantRepository.save(variant);
        }

        log.info("Product created successfully: {} with {} variants", product.getId(), request.getVariants().size());
        return toResponse(product);
    }

    /**
     * Updates an existing product.
     *
     * @param tenantId  the tenant ID
     * @param productId the product ID
     * @param request   the product update request
     * @return the updated product
     * @throws ResourceNotFoundException if product or category not found
     */
    @Transactional
    public ProductResponse updateProduct(Long tenantId, Long productId, ProductRequest request) {
        log.info("Updating product: {} for tenant: {}", productId, tenantId);

        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> {
                    log.warn("Product not found: {} for tenant: {}", productId, tenantId);
                    return new ResourceNotFoundException("Product", productId);
                });

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findByIdAndTenantId(request.getCategoryId(), tenantId)
                    .orElseThrow(() -> {
                        log.error("Category not found: {} for tenant: {}", request.getCategoryId(), tenantId);
                        return new ResourceNotFoundException("Category", request.getCategoryId());
                    });
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(category);

        product = productRepository.save(product);
        log.info("Product updated successfully: {}", product.getId());
        return toResponse(product);
    }

    /**
     * Deletes a product.
     *
     * @param tenantId  the tenant ID
     * @param productId the product ID
     * @throws ResourceNotFoundException if product not found
     */
    @Transactional
    public void deleteProduct(Long tenantId, Long productId) {
        log.info("Deleting product: {} for tenant: {}", productId, tenantId);

        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> {
                    log.warn("Product not found: {} for tenant: {}", productId, tenantId);
                    return new ResourceNotFoundException("Product", productId);
                });
        productRepository.delete(product);
        log.info("Product deleted successfully: {}", productId);
    }

    private ProductResponse toResponse(Product product, List<ProductVariant> variants) {
        List<ProductResponse.VariantResponse> variantResponses = variants.stream()
                .map(v -> ProductResponse.VariantResponse.builder()
                        .id(v.getId())
                        .sku(v.getSku())
                        .price(v.getPrice())
                        .stock(v.getStock())
                        .version(v.getVersion())
                        .build())
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .version(product.getVersion())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .variants(variantResponses)
                .build();
    }

    private ProductResponse toResponse(Product product) {
        List<ProductVariant> variants = variantRepository.findByProductId(product.getId());
        return toResponse(product, variants);
    }
}
