package com.fitgear.backend.order;

import com.fitgear.backend.auth.AppUser;
import com.fitgear.backend.auth.UserRepository;
import com.fitgear.backend.cart.CartItem;
import com.fitgear.backend.cart.CartItemRepository;
import com.fitgear.backend.product.Product;
import com.fitgear.backend.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

class OrderServiceTest {

    @Test
    void shouldRejectEmptyCart() {
        OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
        CartItemRepository cartRepository = Mockito.mock(CartItemRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        AppUser user = new AppUser("Arun", "arun@example.com", "hashed", com.fitgear.backend.auth.UserRole.CUSTOMER);
        Mockito.when(userRepository.findByEmailIgnoreCase("arun@example.com")).thenReturn(Optional.of(user));
        Mockito.when(cartRepository.findByUserEmailIgnoreCase("arun@example.com")).thenReturn(List.of());
        OrderService service = new OrderService(orderRepository, cartRepository, userRepository, productRepository);

        assertThrows(EmptyCartException.class, () -> service.placeOrder("arun@example.com"));
    }

    @Test
    void shouldCreateOrderReduceStockAndClearCart() {
        OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
        CartItemRepository cartRepository = Mockito.mock(CartItemRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        AppUser user = new AppUser("Arun", "arun@example.com", "hashed", com.fitgear.backend.auth.UserRole.CUSTOMER);
        Product product = new Product("Dumbbell", "Training weight", new BigDecimal("1000"), 5);
        CartItem cartItem = new CartItem(user, product, 2);
        Mockito.when(userRepository.findByEmailIgnoreCase("arun@example.com")).thenReturn(Optional.of(user));
        Mockito.when(cartRepository.findByUserEmailIgnoreCase("arun@example.com")).thenReturn(List.of(cartItem));
        Mockito.when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        OrderService service = new OrderService(orderRepository, cartRepository, userRepository, productRepository);

        OrderResponse response = service.placeOrder("arun@example.com");

        assertEquals(new BigDecimal("2000"), response.total());
        assertEquals(OrderStatus.PLACED, response.status());
        assertEquals(3, product.getStock());
        Mockito.verify(cartRepository).deleteAll(List.of(cartItem));
    }

    @Test
    void shouldCancelPlacedOrderAndRestoreStock() {
        OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
        CartItemRepository cartRepository = Mockito.mock(CartItemRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        AppUser user = new AppUser("Arun", "arun@example.com", "hashed", com.fitgear.backend.auth.UserRole.CUSTOMER);
        Product product = new Product("Dumbbell", "Training weight", new BigDecimal("1000"), 3);
        Order order = new Order(user, new BigDecimal("2000"), OrderStatus.PLACED);
        order.addItem(new OrderItem(order, 1L, "Dumbbell", new BigDecimal("1000"), 2));
        Mockito.when(orderRepository.findByIdAndUserEmailIgnoreCase(7L, "arun@example.com")).thenReturn(Optional.of(order));
        Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        Mockito.when(orderRepository.save(order)).thenReturn(order);
        OrderService service = new OrderService(orderRepository, cartRepository, userRepository, productRepository);

        OrderResponse response = service.cancelOrder("arun@example.com", 7L);

        assertEquals(OrderStatus.CANCELLED, response.status());
        assertEquals(5, product.getStock());
    }
}
