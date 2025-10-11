package com.ecommerce.platform;

import com.ecommerce.platform.dto.CartRequest;
import com.ecommerce.platform.dto.ProductRequest;
import com.ecommerce.platform.entity.Tenant;
import com.ecommerce.platform.entity.User;
import com.ecommerce.platform.exception.BadRequestException;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.platform.repository.TenantRepository;
import com.ecommerce.platform.repository.UserRepository;
import com.ecommerce.platform.service.CartService;
import com.ecommerce.platform.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-for-testing-minimum-256-bits-required-here"
})
class CartServiceTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private User customer;
    private Long variantId;
    private Long secondVariantId;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder()
                .name("Cart Store")
                .slug("cart-store")
                .build();
        tenant = tenantRepository.save(tenant);

        customer = User.builder()
                .tenant(tenant)
                .email("customer@cart.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.CUSTOMER)
                .build();
        customer = userRepository.save(customer);

        ProductRequest productRequest = ProductRequest.builder()
                .name("Cart Item Product")
                .description("Product for cart testing")
                .variants(List.of(
                        ProductRequest.VariantRequest.builder()
                                .sku("CART-ITEM-001")
                                .price(BigDecimal.valueOf(49.99))
                                .stock(100)
                                .build()
                ))
                .build();

        var product = productService.createProduct(tenant.getId(), productRequest);
        variantId = product.getVariants().get(0).getId();

        ProductRequest secondProductRequest = ProductRequest.builder()
                .name("Second Product")
                .description("Second product for cart testing")
                .variants(List.of(
                        ProductRequest.VariantRequest.builder()
                                .sku("CART-ITEM-002")
                                .price(BigDecimal.valueOf(29.99))
                                .stock(50)
                                .build()
                ))
                .build();

        var secondProduct = productService.createProduct(tenant.getId(), secondProductRequest);
        secondVariantId = secondProduct.getVariants().get(0).getId();
    }

    @Test
    void getCart_shouldReturnEmptyCartForNewUser() {
        var cart = cartService.getCart(customer.getId());

        assertNotNull(cart);
        assertTrue(cart.getItems().isEmpty());
        assertEquals(BigDecimal.ZERO, cart.getTotal());
    }

    @Test
    void addToCart_shouldAddItem() {
        CartRequest request = CartRequest.builder()
                .variantId(variantId)
                .quantity(2)
                .build();

        var cart = cartService.addToCart(customer.getId(), request);

        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
        assertEquals(BigDecimal.valueOf(99.98), cart.getTotal());
    }

    @Test
    void addToCart_existingItem_shouldIncreaseQuantity() {
        CartRequest request = CartRequest.builder()
                .variantId(variantId)
                .quantity(2)
                .build();

        cartService.addToCart(customer.getId(), request);
        var cart = cartService.addToCart(customer.getId(), request);

        assertEquals(1, cart.getItems().size());
        assertEquals(4, cart.getItems().get(0).getQuantity());
    }

    @Test
    void addToCart_multipleItems_shouldAddAllItems() {
        CartRequest request1 = CartRequest.builder()
                .variantId(variantId)
                .quantity(2)
                .build();

        CartRequest request2 = CartRequest.builder()
                .variantId(secondVariantId)
                .quantity(3)
                .build();

        cartService.addToCart(customer.getId(), request1);
        var cart = cartService.addToCart(customer.getId(), request2);

        assertEquals(2, cart.getItems().size());
        assertEquals(BigDecimal.valueOf(189.95), cart.getTotal()); // 99.98 + 89.97
    }

    @Test
    void addToCart_nonExistentVariant_shouldThrowException() {
        CartRequest request = CartRequest.builder()
                .variantId(99999L)
                .quantity(1)
                .build();

        assertThrows(ResourceNotFoundException.class,
                () -> cartService.addToCart(customer.getId(), request));
    }

    @Test
    void updateCartItem_shouldUpdateQuantity() {
        CartRequest addRequest = CartRequest.builder()
                .variantId(variantId)
                .quantity(1)
                .build();

        var cart = cartService.addToCart(customer.getId(), addRequest);
        Long itemId = cart.getItems().get(0).getId();

        cart = cartService.updateCartItem(customer.getId(), itemId, 3);

        assertEquals(1, cart.getItems().size());
        assertEquals(3, cart.getItems().get(0).getQuantity());
    }

    @Test
    void updateCartItem_quantityZero_shouldRemoveItem() {
        CartRequest addRequest = CartRequest.builder()
                .variantId(variantId)
                .quantity(1)
                .build();

        var cart = cartService.addToCart(customer.getId(), addRequest);
        Long itemId = cart.getItems().get(0).getId();

        cart = cartService.updateCartItem(customer.getId(), itemId, 0);

        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void updateCartItem_negativeQuantity_shouldRemoveItem() {
        CartRequest addRequest = CartRequest.builder()
                .variantId(variantId)
                .quantity(1)
                .build();

        var cart = cartService.addToCart(customer.getId(), addRequest);
        Long itemId = cart.getItems().get(0).getId();

        cart = cartService.updateCartItem(customer.getId(), itemId, -1);

        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void updateCartItem_insufficientStock_shouldThrowException() {
        CartRequest addRequest = CartRequest.builder()
                .variantId(variantId)
                .quantity(1)
                .build();

        var cart = cartService.addToCart(customer.getId(), addRequest);
        Long itemId = cart.getItems().get(0).getId();

        assertThrows(BadRequestException.class,
                () -> cartService.updateCartItem(customer.getId(), itemId, 200));
    }

    @Test
    void updateCartItem_nonExistentItem_shouldThrowException() {
        assertThrows(ResourceNotFoundException.class,
                () -> cartService.updateCartItem(customer.getId(), 99999L, 5));
    }

    @Test
    void updateCartItem_cartNotFound_shouldThrowException() {
        CartRequest addRequest = CartRequest.builder()
                .variantId(variantId)
                .quantity(1)
                .build();

        var cart = cartService.addToCart(customer.getId(), addRequest);
        Long itemId = cart.getItems().get(0).getId();

        // Create another user without cart
        User otherUser = User.builder()
                .tenant(tenant)
                .email("other@cart.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.CUSTOMER)
                .build();
        User savedOtherUser = userRepository.save(otherUser);

        assertThrows(ResourceNotFoundException.class,
                () -> cartService.updateCartItem(savedOtherUser.getId(), itemId, 5));
    }

    @Test
    void removeFromCart_shouldRemoveItem() {
        CartRequest addRequest = CartRequest.builder()
                .variantId(variantId)
                .quantity(1)
                .build();

        var cart = cartService.addToCart(customer.getId(), addRequest);
        Long itemId = cart.getItems().get(0).getId();

        cartService.removeFromCart(customer.getId(), itemId);

        cart = cartService.getCart(customer.getId());
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void removeFromCart_nonExistentItem_shouldThrowException() {
        assertThrows(ResourceNotFoundException.class,
                () -> cartService.removeFromCart(customer.getId(), 99999L));
    }

    @Test
    void removeFromCart_itemNotInUserCart_shouldThrowException() {
        // Create a cart for the customer
        CartRequest addRequest = CartRequest.builder()
                .variantId(variantId)
                .quantity(1)
                .build();
        var cart = cartService.addToCart(customer.getId(), addRequest);
        Long itemId = cart.getItems().get(0).getId();

        // Create another user
        User otherUser = User.builder()
                .tenant(tenant)
                .email("other2@cart.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.CUSTOMER)
                .build();
        User savedOtherUser = userRepository.save(otherUser);

        // First create a cart for the other user
        CartRequest otherRequest = CartRequest.builder()
                .variantId(secondVariantId)
                .quantity(1)
                .build();
        cartService.addToCart(savedOtherUser.getId(), otherRequest);

        // Try to remove customer's item from other user's cart
        assertThrows(BadRequestException.class,
                () -> cartService.removeFromCart(savedOtherUser.getId(), itemId));
    }

    @Test
    void clearCart_shouldRemoveAllItems() {
        CartRequest request1 = CartRequest.builder()
                .variantId(variantId)
                .quantity(2)
                .build();

        CartRequest request2 = CartRequest.builder()
                .variantId(secondVariantId)
                .quantity(3)
                .build();

        cartService.addToCart(customer.getId(), request1);
        cartService.addToCart(customer.getId(), request2);

        cartService.clearCart(customer.getId());

        var cart = cartService.getCart(customer.getId());
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void clearCart_emptyCart_shouldNotThrow() {
        // User has no cart yet
        cartService.clearCart(customer.getId());

        var cart = cartService.getCart(customer.getId());
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void addToCart_shouldFailWithInsufficientStock() {
        CartRequest request = CartRequest.builder()
                .variantId(variantId)
                .quantity(200) // More than available stock (100)
                .build();

        assertThrows(BadRequestException.class, () -> cartService.addToCart(customer.getId(), request));
    }

    @Test
    void addToCart_existingItemInsufficientStock_shouldFail() {
        CartRequest request = CartRequest.builder()
                .variantId(variantId)
                .quantity(80)
                .build();

        cartService.addToCart(customer.getId(), request);

        // Try to add more, which would exceed stock
        CartRequest secondRequest = CartRequest.builder()
                .variantId(variantId)
                .quantity(30)
                .build();

        assertThrows(BadRequestException.class,
                () -> cartService.addToCart(customer.getId(), secondRequest));
    }

    @Test
    void updateCartItem_itemBelongsToOtherCart_shouldThrowException() {
        // Create first user and add item
        CartRequest addRequest = CartRequest.builder()
                .variantId(variantId)
                .quantity(1)
                .build();
        var cart = cartService.addToCart(customer.getId(), addRequest);
        Long itemId = cart.getItems().get(0).getId();

        // Create second user
        User otherUser = User.builder()
                .tenant(tenant)
                .email("other3@cart.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.CUSTOMER)
                .build();
        User savedOtherUser = userRepository.save(otherUser);

        // Create cart for second user
        CartRequest otherRequest = CartRequest.builder()
                .variantId(secondVariantId)
                .quantity(1)
                .build();
        cartService.addToCart(savedOtherUser.getId(), otherRequest);

        // Try to update first user's item with second user
        assertThrows(BadRequestException.class,
                () -> cartService.updateCartItem(savedOtherUser.getId(), itemId, 5));
    }
}
