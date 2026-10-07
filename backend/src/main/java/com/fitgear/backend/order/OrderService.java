package com.fitgear.backend.order;

import com.fitgear.backend.auth.AppUser;
import com.fitgear.backend.auth.UserRepository;
import com.fitgear.backend.cart.CartItem;
import com.fitgear.backend.cart.CartItemRepository;
import com.fitgear.backend.cart.InsufficientStockException;
import com.fitgear.backend.product.Product;
import com.fitgear.backend.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, CartItemRepository cartItemRepository, UserRepository userRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse placeOrder(String email) {
        AppUser user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        List<CartItem> cartItems = cartItemRepository.findByUserEmailIgnoreCase(email);
        if (cartItems.isEmpty()) throw new EmptyCartException();

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (cartItem.getQuantity() > product.getStock()) {
                throw new InsufficientStockException(product.getName(), cartItem.getQuantity(), product.getStock());
            }
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        Order order = new Order(user, total, OrderStatus.PLACED);
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            product.setStock(product.getStock() - cartItem.getQuantity());
            order.addItem(new OrderItem(order, product.getId(), product.getName(), product.getPrice(), cartItem.getQuantity()));
        }
        Order savedOrder = orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);
        return OrderResponse.from(savedOrder);
    }

    public List<OrderResponse> getOrders(String email) {
        return orderRepository.findByUserEmailIgnoreCaseOrderByCreatedAtDesc(email).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional
    public OrderResponse cancelOrder(String email, Long orderId) {
        Order order = orderRepository.findByIdAndUserEmailIgnoreCase(orderId, email)
                .orElseThrow(() -> new OrderCancellationException("Order not found: " + orderId));
        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new OrderCancellationException("Order cannot be cancelled after it is packed");
        }
        order.getItems().forEach(item -> productRepository.findById(item.getProductId())
                .ifPresent(product -> product.setStock(product.getStock() + item.getQuantity())));
        order.setStatus(OrderStatus.CANCELLED);
        return OrderResponse.from(orderRepository.save(order));
    }
}
