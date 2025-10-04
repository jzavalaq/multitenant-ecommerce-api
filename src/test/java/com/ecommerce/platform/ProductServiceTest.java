package com.ecommerce.platform;

import com.ecommerce.platform.dto.ProductRequest;
import com.ecommerce.platform.entity.Category;
import com.ecommerce.platform.entity.Tenant;
import com.ecommerce.platform.exception.BadRequestException;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.platform.repository.CategoryRepository;
import com.ecommerce.platform.repository.ProductRepository;
import com.ecommerce.platform.repository.ProductVariantRepository;
import com.ecommerce.platform.repository.TenantRepository;
import com.ecommerce.platform.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-for-testing-minimum-256-bits-required-here"
})
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    private Tenant tenant;
    private Category category;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder()
                .name("Product Store")
                .slug("product-store")
                .build();
        tenant = tenantRepository.save(tenant);

        category = Category.builder()
                .tenant(tenant)
                .name("Electronics")
                .build();
        category = categoryRepository.save(category);
    }

    @Test
    void createProduct_shouldReturnCreatedProduct() {
        ProductRequest request = ProductRequest.builder()
                .name("Laptop")
                .description("Gaming laptop")
                .categoryId(category.getId())
                .variants(List.of(
                        ProductRequest.VariantRequest.builder()
                                .sku("LAPTOP-001")
                                .price(BigDecimal.valueOf(999.99))
                                .stock(10)
                                .build()
                ))
                .build();

        var response = productService.createProduct(tenant.getId(), request);

        assertNotNull(response.getId());
        assertEquals("Laptop", response.getName());
        assertEquals("Gaming laptop", response.getDescription());
        assertEquals(1, response.getVariants().size());
        assertEquals("LAPTOP-001", response.getVariants().get(0).getSku());
    }

    @Test
    void createProduct_withoutCategory_shouldReturnCreatedProduct() {
        ProductRequest request = ProductRequest.builder()
                .name("Laptop")
                .description("Gaming laptop")
                .variants(List.of(
                        ProductRequest.VariantRequest.builder()
                                .sku("LAPTOP-002")
                                .price(BigDecimal.valueOf(999.99))
                                .stock(10)
                                .build()
                ))
                .build();

        var response = productService.createProduct(tenant.getId(), request);

        assertNotNull(response.getId());
        assertEquals("Laptop", response.getName());
        assertNull(response.getCategoryId());
    }

    @Test
    void createProduct_multipleVariants_shouldReturnCreatedProduct() {
        ProductRequest request = ProductRequest.builder()
                .name("T-Shirt")
                .description("Cotton t-shirt")
                .categoryId(category.getId())
                .variants(List.of(
                        ProductRequest.VariantRequest.builder()
                                .sku("TSHIRT-S")
                                .price(BigDecimal.valueOf(19.99))
                                .stock(50)
                                .build(),
                        ProductRequest.VariantRequest.builder()
                                .sku("TSHIRT-M")
                                .price(BigDecimal.valueOf(19.99))
                                .stock(40)
                                .build(),
                        ProductRequest.VariantRequest.builder()
                                .sku("TSHIRT-L")
                                .price(BigDecimal.valueOf(19.99))
                                .stock(30)
                                .build()
                ))
                .build();

        var response = productService.createProduct(tenant.getId(), request);

        assertNotNull(response.getId());
        assertEquals(3, response.getVariants().size());
    }

    @Test
    void createProduct_invalidCategory_shouldThrowException() {
        ProductRequest request = ProductRequest.builder()
                .name("Laptop")
                .description("Gaming laptop")
                .categoryId(99999L)
                .variants(List.of(
                        ProductRequest.VariantRequest.builder()
                                .sku("LAPTOP-003")
                                .price(BigDecimal.valueOf(999.99))
                                .stock(10)
                                .build()
                ))
                .build();

        assertThrows(ResourceNotFoundException.class,
                () -> productService.createProduct(tenant.getId(), request));
    }

    @Test
    void getProducts_shouldReturnPagedProducts() {
        ProductRequest request = ProductRequest.builder()
                .name("Phone")
                .description("Smartphone")
                .variants(List.of(
                        ProductRequest.VariantRequest.builder()
                                .sku("PHONE-001")
                                .price(BigDecimal.valueOf(499.99))
                                .stock(20)
                                .build()
                ))
                .build();

        productService.createProduct(tenant.getId(), request);

        var response = productService.getProducts(tenant.getId(), 0, 20, null);

        assertEquals(1, response.getContent().size());
        assertEquals("Phone", response.getContent().get(0).getName());
    }

    @Test
    void getProducts_withCategoryFilter_shouldReturnFilteredProducts() {
        Category otherCategory = Category.builder()
                .tenant(tenant)
                .name("Clothing")
                .build();
        otherCategory = categoryRepository.save(otherCategory);

        ProductRequest request1 = ProductRequest.builder()
                .name("Phone")
                .description("Smartphone")
                .categoryId(category.getId())
                .variants(List.of(
                        ProductRequest.VariantRequest.builder()
                                .sku("PHONE-002")
                                .price(BigDecimal.valueOf(499.99))
                                .stock(20)
                                .build()
                ))
                .build();

        ProductRequest request2 = ProductRequest.builder()
                .name("Shirt")
                .description("T-Shirt")
                .categoryId(otherCategory.getId())
                .variants(List.of(
                        ProductRequest.VariantRequest.builder()
                                .sku("SHIRT-001")
                                .price(BigDecimal.valueOf(29.99))
                                .stock(50)
                                .build()
                ))
                .build();

        productService.createProduct(tenant.getId(), request1);
        productService.createProduct(tenant.getId(), request2);

        var response = productService.getProducts(tenant.getId(), 0, 20, category.getId());

        assertEquals(1, response.getContent().size());
        assertEquals("Phone", response.getContent().get(0).getName());
    }

    @Test
    void getProducts_invalidPagination_shouldThrowException() {
        assertThrows(BadRequestException.class,
                () -> productService.getProducts(tenant.getId(), -1, 20, null));

        assertThrows(BadRequestException.class,
                () -> productService.getProducts(tenant.getId(), 0, 0, null));
    }

    @Test
    void getProducts_emptyResult_shouldReturnEmptyPage() {
        var response = productService.getProducts(tenant.getId(), 0, 20, null);

        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotalElements());
    }

    @Test
    void getProduct_shouldReturnProductById() {
        ProductRequest request = ProductRequest.builder()
                .name("Tablet")
                .description("Android tablet")
                .variants(List.of(
                        ProductRequest.VariantRequest.builder()
                                .sku("TABLET-001")
                                .price(BigDecimal.valueOf(299.99))
                                .stock(15)
                                .build()
                ))
                .build();

        var created = productService.createProduct(tenant.getId(), request);

        var response = productService.getProduct(tenant.getId(), created.getId());

        assertEquals("Tablet", response.getName());
        assertEquals("Android tablet", response.getDescription());
    }

    @Test
    void getProduct_nonExistentId_shouldThrowException() {
        assertThrows(ResourceNotFoundException.class,
                () -> productService.getProduct(tenant.getId(), 99999L));
    }

    @Test
    void updateProduct_existingId_shouldReturnUpdatedProduct() {
        ProductRequest createRequest = ProductRequest.builder()
                .name("Tablet")
                .description("Android tablet")
                .variants(List.of(
                        ProductRequest.VariantRequest.builder()
                                .sku("TABLET-002")
                                .price(BigDecimal.valueOf(299.99))
                                .stock(15)
                                .build()
                ))
                .build();

        var created = productService.createProduct(tenant.getId(), createRequest);

        ProductRequest updateRequest = ProductRequest.builder()
                .name("Updated Tablet")
                .description("Updated description")
                .categoryId(category.getId())
                .build();

        var response = productService.updateProduct(tenant.getId(), created.getId(), updateRequest);

        assertEquals("Updated Tablet", response.getName());
        assertEquals("Updated description", response.getDescription());
        assertEquals(category.getId(), response.getCategoryId());
    }

    @Test
    void updateProduct_nonExistentId_shouldThrowException() {
        ProductRequest request = ProductRequest.builder()
                .name("Tablet")
                .description("Android tablet")
                .build();

        assertThrows(ResourceNotFoundException.class,
                () -> productService.updateProduct(tenant.getId(), 99999L, request));
    }

    @Test
    void updateProduct_invalidCategory_shouldThrowException() {
        ProductRequest createRequest = ProductRequest.builder()
                .name("Tablet")
                .description("Android tablet")
                .variants(List.of(
                        ProductRequest.VariantRequest.builder()
                                .sku("TABLET-003")
                                .price(BigDecimal.valueOf(299.99))
                                .stock(15)
                                .build()
                ))
                .build();

        var created = productService.createProduct(tenant.getId(), createRequest);

        ProductRequest updateRequest = ProductRequest.builder()
                .name("Updated Tablet")
                .description("Updated description")
                .categoryId(99999L)
                .build();

        assertThrows(ResourceNotFoundException.class,
                () -> productService.updateProduct(tenant.getId(), created.getId(), updateRequest));
    }

    @Test
    void deleteProduct_shouldRemoveProduct() {
        ProductRequest request = ProductRequest.builder()
                .name("To Delete")
                .description("Product to delete")
                .variants(List.of(
                        ProductRequest.VariantRequest.builder()
                                .sku("DELETE-001")
                                .price(BigDecimal.valueOf(99.99))
                                .stock(5)
                                .build()
                ))
                .build();

        var created = productService.createProduct(tenant.getId(), request);
        Long productId = created.getId();

        // First delete the product variants
        productVariantRepository.deleteAll();

        // Now delete the product
        productService.deleteProduct(tenant.getId(), productId);

        // Verify the product no longer exists
        assertThrows(Exception.class, () -> productService.getProduct(tenant.getId(), productId));
    }

    @Test
    void deleteProduct_nonExistentId_shouldThrowException() {
        assertThrows(ResourceNotFoundException.class,
                () -> productService.deleteProduct(tenant.getId(), 99999L));
    }
}
