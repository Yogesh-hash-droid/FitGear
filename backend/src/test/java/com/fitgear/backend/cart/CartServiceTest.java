package com.fitgear.backend.cart;

import com.fitgear.backend.auth.AppUser;
import com.fitgear.backend.auth.UserRepository;
import com.fitgear.backend.auth.UserRole;
import com.fitgear.backend.product.Product;
import com.fitgear.backend.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

class CartServiceTest {

    @Test
    void shouldRejectQuantityAboveStock() {
        CartItemRepository cartRepository = Mockito.mock(CartItemRepository.class);
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        Product product = new Product("Dumbbell", "Training weight", new BigDecimal("1000"), 2);
        AppUser user = new AppUser("Arun", "arun@example.com", "hashed", UserRole.CUSTOMER);
        Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        Mockito.when(userRepository.findByEmailIgnoreCase("arun@example.com")).thenReturn(Optional.of(user));
        Mockito.when(cartRepository.findByUserEmailIgnoreCaseAndProductId("arun@example.com", 1L)).thenReturn(Optional.empty());
        CartService service = new CartService(cartRepository, productRepository, userRepository);

        assertThrows(InsufficientStockException.class, () -> service.addItem(
                "arun@example.com", new CartItemRequest(1L, 3)
        ));
    }

    @Test
    void shouldSaveCartItemForAuthenticatedUser() {
        CartItemRepository cartRepository = Mockito.mock(CartItemRepository.class);
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        Product product = new Product("Dumbbell", "Training weight", new BigDecimal("1000"), 5);
        AppUser user = new AppUser("Arun", "arun@example.com", "hashed", UserRole.CUSTOMER);
        Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        Mockito.when(userRepository.findByEmailIgnoreCase("arun@example.com")).thenReturn(Optional.of(user));
        Mockito.when(cartRepository.findByUserEmailIgnoreCaseAndProductId("arun@example.com", 1L)).thenReturn(Optional.empty());
        Mockito.when(cartRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CartService service = new CartService(cartRepository, productRepository, userRepository);

        service.addItem("arun@example.com", new CartItemRequest(1L, 2));

        Mockito.verify(cartRepository).save(any(CartItem.class));
    }
}