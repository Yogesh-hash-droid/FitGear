package com.fitgear.backend.order;

import com.fitgear.backend.auth.AppUser;
import com.fitgear.backend.auth.UserRepository;
import com.fitgear.backend.auth.UserRole;
import com.fitgear.backend.cart.CartItem;
import com.fitgear.backend.cart.CartItemRepository;
import com.fitgear.backend.cart.InsufficientStockException;
import com.fitgear.backend.notification.NotificationService;
import com.fitgear.backend.payment.PaymentMethod;
import com.fitgear.backend.payment.PaymentStatus;
import com.fitgear.backend.product.Product;
import com.fitgear.backend.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private CartItemRepository cartRepository;
    private UserRepository userRepository;
    private ProductRepository productRepository;
    private NotificationService notificationService;

    private final AppUser user = new AppUser("Arun", "arun@example.com", "hashed", UserRole.CUSTOMER);

    @BeforeEach
    void setUp() {
        orderRepository = Mockito.mock(OrderRepository.class);
        cartRepository = Mockito.mock(CartItemRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        productRepository = Mockito.mock(ProductRepository.class);
        notificationService = Mockito.mock(NotificationService.class);

        Mockito.when(userRepository.findByEmailIgnoreCase("arun@example.com")).thenReturn(Optional.of(user));
        Mockito.when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private OrderService service() {
        return new OrderService(orderRepository, cartRepository, userRepository, productRepository, notificationService);
    }

    private OrderRequest request(PaymentMethod method) {
        return new OrderRequest(
                new OrderRequest.AddressRequest(
                        "Arun", "9876543210", "12 Temple Street", null, "Chennai", "Tamil Nadu", "600001"),
                method
        );
    }

    @Test
    void shouldRejectEmptyCart() {
        Mockito.when(cartRepository.findByUserEmailIgnoreCase("arun@example.com")).thenReturn(List.of());

        assertThrows(EmptyCartException.class, () -> service().placeOrder("arun@example.com", request(PaymentMethod.COD)));
    }

    @Test
    void shouldPlaceCodOrderConfirmImmediatelyAndReserveStock() {
        Product product = new Product("Dumbbell", "Training weight", new BigDecimal("1000"), 5);
        product.setId(1L);
        CartItem cartItem = new CartItem(user, product, 2);
        Mockito.when(cartRepository.findByUserEmailIgnoreCase("arun@example.com")).thenReturn(List.of(cartItem));
        Mockito.when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        OrderResponse response = service().placeOrder("arun@example.com", request(PaymentMethod.COD));

        assertEquals(OrderStatus.CONFIRMED, response.status());
        assertEquals(new BigDecimal("2000"), response.total());
        assertEquals(new BigDecimal("2000"), response.subtotal());
        assertEquals(3, product.getStock());
        assertEquals(PaymentMethod.COD, response.payment().method());
        assertEquals(PaymentStatus.PENDING, response.payment().status());
        assertEquals("Chennai", response.address().city());
        Mockito.verify(cartRepository).deleteAll(List.of(cartItem));
    }

    @Test
    void shouldPlaceRazorpayOrderAsPlacedAwaitingPayment() {
        Product product = new Product("Dumbbell", "Training weight", new BigDecimal("1000"), 5);
        product.setId(1L);
        CartItem cartItem = new CartItem(user, product, 2);
        Mockito.when(cartRepository.findByUserEmailIgnoreCase("arun@example.com")).thenReturn(List.of(cartItem));
        Mockito.when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        OrderResponse response = service().placeOrder("arun@example.com", request(PaymentMethod.RAZORPAY));

        assertEquals(OrderStatus.PLACED, response.status());
        assertEquals(PaymentMethod.RAZORPAY, response.payment().method());
        assertEquals(3, product.getStock());
        assertEquals(true, response.cancelEligible());
    }

    @Test
    void shouldRejectQuantityAboveStockDuringCheckout() {
        Product product = new Product("Dumbbell", "Training weight", new BigDecimal("1000"), 2);
        product.setId(1L);
        CartItem cartItem = new CartItem(user, product, 3);
        Mockito.when(cartRepository.findByUserEmailIgnoreCase("arun@example.com")).thenReturn(List.of(cartItem));
        Mockito.when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class,
                () -> service().placeOrder("arun@example.com", request(PaymentMethod.COD)));
    }

    @Test
    void shouldCancelOrderAndRestoreStock() {
        Product product = new Product("Dumbbell", "Training weight", new BigDecimal("1000"), 3);
        product.setId(1L);
        Order order = new Order(user, OrderStatus.CONFIRMED, new BigDecimal("2000"), new BigDecimal("2000"),
                new OrderAddress("Arun", "9876543210", "12 Temple Street", null, "Chennai", "Tamil Nadu", "600001"));
        order.addItem(new OrderItem(order, 1L, "Dumbbell", "/img.svg", new BigDecimal("1000"), 2));
        order.setPayment(new com.fitgear.backend.payment.Payment(
                order, PaymentMethod.RAZORPAY, PaymentStatus.PAID, new BigDecimal("2000")));

        Mockito.when(orderRepository.findByIdAndUserEmailIgnoreCase(7L, "arun@example.com")).thenReturn(Optional.of(order));
        Mockito.when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        OrderResponse response = service().cancelOrder("arun@example.com", 7L);

        assertEquals(OrderStatus.CANCELLED, response.status());
        assertEquals(5, product.getStock());
        assertEquals(PaymentStatus.REFUNDED, response.payment().status());
        Mockito.verify(notificationService).publish(anyString(), anyString(), any());
    }

    @Test
    void adminShouldAdvanceOrderThroughStatusLifecycle() {
        Order order = new Order(user, OrderStatus.CONFIRMED, new BigDecimal("2000"), new BigDecimal("2000"),
                new OrderAddress("Arun", "9876543210", "12 Temple Street", null, "Chennai", "Tamil Nadu", "600001"));
        Mockito.when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        OrderResponse processing = service().updateOrderStatus(7L, OrderStatus.PROCESSING);
        assertEquals(OrderStatus.PROCESSING, processing.status());

        OrderResponse shipped = service().updateOrderStatus(7L, OrderStatus.SHIPPED);
        assertEquals(OrderStatus.SHIPPED, shipped.status());

        OrderResponse delivered = service().updateOrderStatus(7L, OrderStatus.DELIVERED);
        assertEquals(OrderStatus.DELIVERED, delivered.status());
        assertEquals(false, delivered.cancelEligible());
    }

    @Test
    void adminShouldRejectInvalidStatusTransition() {
        Order order = new Order(user, OrderStatus.SHIPPED, new BigDecimal("2000"), new BigDecimal("2000"),
                new OrderAddress("Arun", "9876543210", "12 Temple Street", null, "Chennai", "Tamil Nadu", "600001"));
        Mockito.when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderTransitionException.class,
                () -> service().updateOrderStatus(7L, OrderStatus.CANCELLED));
    }

    @Test
    void deliveringCodOrderShouldCapturePayment() {
        Order order = new Order(user, OrderStatus.SHIPPED, new BigDecimal("1500"), new BigDecimal("1500"),
                new OrderAddress("Arun", "9876543210", "12 Temple Street", null, "Chennai", "Tamil Nadu", "600001"));
        var payment = new com.fitgear.backend.payment.Payment(order, PaymentMethod.COD, PaymentStatus.PENDING, new BigDecimal("1500"));
        order.setPayment(payment);
        Mockito.when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        service().updateOrderStatus(7L, OrderStatus.DELIVERED);

        assertEquals(PaymentStatus.PAID, payment.getStatus());
    }
}
