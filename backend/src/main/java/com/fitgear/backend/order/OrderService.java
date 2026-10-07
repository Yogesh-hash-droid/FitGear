package com.fitgear.backend.order;

import com.fitgear.backend.auth.AppUser;
import com.fitgear.backend.auth.UserNotFoundException;
import com.fitgear.backend.auth.UserRepository;
import com.fitgear.backend.cart.CartItem;
import com.fitgear.backend.cart.CartItemRepository;
import com.fitgear.backend.cart.InsufficientStockException;
import com.fitgear.backend.notification.NotificationService;
import com.fitgear.backend.payment.Payment;
import com.fitgear.backend.payment.PaymentMethod;
import com.fitgear.backend.payment.PaymentStatus;
import com.fitgear.backend.product.Product;
import com.fitgear.backend.product.ProductNotFoundException;
import com.fitgear.backend.product.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Order lifecycle:
 *
 * <pre>
 * RAZORPAY:  cart -> PLACED --(payment verified)--> CONFIRMED -> PROCESSING -> SHIPPED -> DELIVERED
 * COD:       cart -> CONFIRMED -> PROCESSING -> SHIPPED -> DELIVERED (payment captured on delivery)
 * cancel:    allowed from PLACED / CONFIRMED / PROCESSING (restores stock)
 * </pre>
 *
 * Stock is reserved when the order is placed (row locks prevent overselling)
 * and restored if the order is cancelled.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PLACED, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED)
    );

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public OrderService(
            OrderRepository orderRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            NotificationService notificationService
    ) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public OrderResponse placeOrder(String email, OrderRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));

        List<CartItem> cartItems = cartItemRepository.findByUserEmailIgnoreCase(email);
        if (cartItems.isEmpty()) {
            throw new EmptyCartException();
        }

        // Lock products in a stable order (by id) to avoid deadlocks between
        // concurrent checkouts sharing product lines.
        List<CartItem> items = new java.util.ArrayList<>(cartItems);
        items.sort(Comparator.comparing(item -> item.getProduct().getId()));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : items) {
            Product product = lockProduct(cartItem.getProduct().getId());
            if (cartItem.getQuantity() > product.getStock()) {
                throw new InsufficientStockException(product.getName(), cartItem.getQuantity(), product.getStock());
            }
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        PaymentMethod method = request.paymentMethod();
        OrderStatus initialStatus = method == PaymentMethod.COD ? OrderStatus.CONFIRMED : OrderStatus.PLACED;
        Order order = new Order(user, initialStatus, subtotal, subtotal, request.address().toOrderAddress());

        for (CartItem cartItem : items) {
            Product product = lockProduct(cartItem.getProduct().getId());
            product.setStock(product.getStock() - cartItem.getQuantity());
            order.addItem(new OrderItem(
                    order, product.getId(), product.getName(), product.getImageUrl(),
                    product.getPrice(), cartItem.getQuantity()
            ));
        }

        order.setPayment(new Payment(order, method, PaymentStatus.PENDING, subtotal));

        Order savedOrder = orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);
        log.info("Order {} placed for {} ({} items, total {}, {})",
                savedOrder.getId(), email, savedOrder.getItems().size(), subtotal, method);
        return OrderResponse.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(String email) {
        return orderRepository.findByUserEmailIgnoreCaseOrderByCreatedAtDesc(email).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String email, Long orderId) {
        return orderRepository.findByIdAndUserEmailIgnoreCase(orderId, email)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional
    public OrderResponse cancelOrder(String email, Long orderId) {
        Order order = orderRepository.findByIdAndUserEmailIgnoreCase(orderId, email)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!OrderResponse.isCancelEligible(order.getStatus())) {
            throw new OrderCancellationException(
                    "Order can no longer be cancelled once it has been shipped");
        }
        cancel(order, "Order cancelled by customer");
        log.info("Order {} cancelled by {}", orderId, email);
        return OrderResponse.from(order);
    }

    // ------------------------------------------------------------------
    // Admin operations
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        Page<Order> page = status == null
                ? orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                : orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return page.map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderDetails(Long orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(order.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new InvalidOrderTransitionException(
                    "Cannot move order from " + order.getStatus() + " to " + newStatus);
        }

        if (newStatus == OrderStatus.CANCELLED) {
            cancel(order, "Order cancelled by admin");
        } else {
            order.setStatus(newStatus);
            if (newStatus == OrderStatus.DELIVERED) {
                captureCodPayment(order);
            }
            orderRepository.save(order);
            notifyCustomer(order, "Order status updated to " + newStatus.name());
        }
        log.info("Order {} moved to {} by admin", orderId, newStatus);
        return OrderResponse.from(order);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private void cancel(Order order, String message) {
        order.getItems().forEach(item -> {
            Product product = lockProduct(item.getProductId());
            product.setStock(product.getStock() + item.getQuantity());
        });
        Payment payment = order.getPayment();
        if (payment != null) {
            switch (payment.getStatus()) {
                // Money was captured - flag for refund (manual refund via Razorpay dashboard)
                case PAID -> payment.setStatus(PaymentStatus.REFUNDED);
                case PENDING, FAILED -> payment.setStatus(PaymentStatus.CANCELLED);
                default -> { /* REFUNDED / CANCELLED are terminal */ }
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        notifyCustomer(order, message);
    }

    private void captureCodPayment(Order order) {
        Payment payment = order.getPayment();
        if (payment != null && payment.getMethod() == PaymentMethod.COD
                && payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.PAID);
        }
    }

    private Product lockProduct(Long productId) {
        return productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private void notifyCustomer(Order order, String message) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("orderId", order.getId());
        payload.put("status", order.getStatus().name());
        payload.put("message", message);
        notificationService.publish(order.getUser().getEmail(), "ORDER_STATUS_CHANGED", payload);
    }
}
