package com.ecommerce.platform;

import com.ecommerce.platform.dto.CategoryRequest;
import com.ecommerce.platform.entity.Category;
import com.ecommerce.platform.entity.Tenant;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.platform.repository.CategoryRepository;
import com.ecommerce.platform.repository.TenantRepository;
import com.ecommerce.platform.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-for-testing-minimum-256-bits-required-here"
})
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder()
                .name("Category Store")
                .slug("category-store")
                .build();
        tenant = tenantRepository.save(tenant);
    }

    @Test
    void getCategories_shouldReturnEmptyListForNewTenant() {
        var categories = categoryService.getCategories(tenant.getId(), 0, 20);

        assertNotNull(categories);
        assertTrue(categories.getContent().isEmpty());
    }

    @Test
    void createCategory_shouldReturnCreatedCategory() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Electronics")
                .build();

        var response = categoryService.createCategory(tenant.getId(), request);

        assertNotNull(response.getId());
        assertEquals("Electronics", response.getName());
        assertNull(response.getParentId());
    }

    @Test
    void createCategory_withParent_shouldCreateHierarchicalCategory() {
        Category parent = Category.builder()
                .tenant(tenant)
                .name("Electronics")
                .build();
        parent = categoryRepository.save(parent);

        CategoryRequest request = CategoryRequest.builder()
                .name("Smartphones")
                .parentId(parent.getId())
                .build();

        var response = categoryService.createCategory(tenant.getId(), request);

        assertNotNull(response.getId());
        assertEquals("Smartphones", response.getName());
        assertEquals(parent.getId(), response.getParentId());
    }

    @Test
    void createCategory_invalidParent_shouldThrowException() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Smartphones")
                .parentId(99999L)
                .build();

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.createCategory(tenant.getId(), request));
    }

    @Test
    void getCategory_existingId_returnsCategory() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Electronics")
                .build();
        var created = categoryService.createCategory(tenant.getId(), request);

        var response = categoryService.getCategory(tenant.getId(), created.getId());

        assertEquals("Electronics", response.getName());
    }

    @Test
    void getCategory_nonExistentId_throwsException() {
        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.getCategory(tenant.getId(), 99999L));
    }

    @Test
    void updateCategory_existingId_returnsUpdatedCategory() {
        CategoryRequest createRequest = CategoryRequest.builder()
                .name("Electronics")
                .build();
        var created = categoryService.createCategory(tenant.getId(), createRequest);

        CategoryRequest updateRequest = CategoryRequest.builder()
                .name("Electronic Devices")
                .build();

        var response = categoryService.updateCategory(tenant.getId(), created.getId(), updateRequest);

        assertEquals("Electronic Devices", response.getName());
    }

    @Test
    void updateCategory_nonExistentId_throwsException() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Electronics")
                .build();

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.updateCategory(tenant.getId(), 99999L, request));
    }

    @Test
    void deleteCategory_existingId_removesCategory() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Electronics")
                .build();
        var created = categoryService.createCategory(tenant.getId(), request);

        categoryService.deleteCategory(tenant.getId(), created.getId());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.getCategory(tenant.getId(), created.getId()));
    }

    @Test
    void deleteCategory_nonExistentId_throwsException() {
        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.deleteCategory(tenant.getId(), 99999L));
    }

    @Test
    void getCategories_shouldReturnAllCategories() {
        CategoryRequest request1 = CategoryRequest.builder().name("Electronics").build();
        CategoryRequest request2 = CategoryRequest.builder().name("Clothing").build();

        categoryService.createCategory(tenant.getId(), request1);
        categoryService.createCategory(tenant.getId(), request2);

        var categories = categoryService.getCategories(tenant.getId(), 0, 20);

        assertEquals(2, categories.getContent().size());
    }
}
