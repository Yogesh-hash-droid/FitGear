package com.fitgear.backend.payment;

import com.fitgear.backend.notification.NotificationService;
import com.fitgear.backend.order.Order;
import com.fitgear.backend.order.OrderNotFoundException;
import com.fitgear.backend.order.OrderRepository;
import com.fitgear.backend.order.OrderResponse;
import com.fitgear.backend.order.OrderStatus;
import com.fitgear.backend.util.SignatureUtils;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Razorpay integration (test mode during development).
 *
 * <p>Flow: browser asks for a Razorpay order -> pays in the Razorpay widget ->
 * sends the widget's signature back -> <b>server</b> recomputes the HMAC and only
 * then confirms the order. A webhook (payment.captured / payment.failed) provides
 * a second, idempotent confirmation path for production.</p>
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final BigDecimal PAISE = BigDecimal.valueOf(100);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;

    public PaymentService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            NotificationService notificationService,
            ObjectMapper objectMapper,
            @Value("${fitgear.razorpay.key-id:}") String keyId,
            @Value("${fitgear.razorpay.key-secret:}") String keySecret,
            @Value("${fitgear.razorpay.webhook-secret:}") String webhookSecret
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
    }

    public PaymentConfigResponse config() {
        return new PaymentConfigResponse(isConfigured(), isConfigured() ? keyId : "");
    }

    public boolean isConfigured() {
        return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
    }

    @Transactional
    public CheckoutResponse createCheckout(String email, Long orderId) {
        if (!isConfigured()) {
            throw new PaymentNotConfiguredException();
        }
        Order order = loadOwnableOrder(email, orderId);
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new PaymentException("Order is not awaiting payment (status: " + order.getStatus() + ")");
        }
        Payment payment = requirePayment(order);
        if (payment.getMethod() != PaymentMethod.RAZORPAY) {
            throw new PaymentException("This order was placed with Cash on Delivery");
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new PaymentException("Order is already paid");
        }

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject attributes = new JSONObject();
            attributes.put("amount", toPaise(order.getTotal()));
            attributes.put("currency", "INR");
            attributes.put("receipt", "fitgear-order-" + order.getId());

            com.razorpay.Order razorpayOrder = client.orders.create(attributes);
            payment.setRazorpayOrderId(razorpayOrder.get("id").toString());
            payment.setAmount(order.getTotal());
            payment.setStatus(PaymentStatus.PENDING);
            paymentRepository.save(payment);

            return new CheckoutResponse(
                    keyId,
                    payment.getRazorpayOrderId(),
                    toPaise(order.getTotal()),
                    "INR",
                    order.getId(),
                    order.getUser().getName(),
                    order.getUser().getEmail()
            );
        } catch (Exception e) {
            log.error("Failed to create Razorpay order for order {}", orderId, e);
            throw new PaymentException("Unable to initialize online payment: " + e.getMessage());
        }
    }

    @Transactional
    public OrderResponse verifyPayment(String email, VerifyPaymentRequest request) {
        if (!isConfigured()) {
            throw new PaymentNotConfiguredException();
        }
        Order order = loadOwnableOrder(email, request.orderId());
        Payment payment = requirePayment(order);

        if (!request.razorpayOrderId().equals(payment.getRazorpayOrderId())) {
            throw new PaymentVerificationException("Payment reference does not match this order");
        }

        String expected = SignatureUtils.hmacSha256Hex(
                keySecret, request.razorpayOrderId() + "|" + request.razorpayPaymentId());
        if (!SignatureUtils.constantTimeEquals(expected, request.razorpaySignature())) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.warn("Payment signature verification failed for order {}", order.getId());
            throw new PaymentVerificationException("Payment signature verification failed");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setRazorpaySignature(request.razorpaySignature());
        confirmOrder(order, "Payment confirmed");
        log.info("Payment verified for order {}", order.getId());
        return OrderResponse.from(order);
    }

    /**
     * Razorpay webhook: signed with the webhook secret using HMAC-SHA256 of the
     * raw request body. Confirmation here is idempotent - it is the safety net
     * if the browser never calls /verify.
     */
    @Transactional
    public void handleWebhook(String rawBody, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new PaymentNotConfiguredException();
        }
        String expected = SignatureUtils.hmacSha256Hex(webhookSecret, rawBody);
        if (!SignatureUtils.constantTimeEquals(expected, signature)) {
            log.warn("Rejected webhook with invalid signature");
            throw new PaymentVerificationException("Invalid webhook signature");
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String event = root.path("event").asText("");
            JsonNode entity = root.path("payload").path("payment").path("entity");
            String razorpayOrderId = entity.path("order_id").asText(null);
            String razorpayPaymentId = entity.path("id").asText(null);
            if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
                return;
            }

            Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
            if (payment == null) {
                log.info("Webhook references unknown Razorpay order {}", razorpayOrderId);
                return;
            }

            switch (event) {
                case "payment.captured" -> capture(payment, razorpayPaymentId, "Payment confirmed");
                case "payment.failed" -> fail(payment, razorpayPaymentId);
                default -> log.debug("Ignored webhook event {}", event);
            }
        } catch (JacksonException e) {
            throw new PaymentVerificationException("Malformed webhook payload");
        }
    }

    private void capture(Payment payment, String razorpayPaymentId, String message) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            return; // already confirmed by /verify - keep the operation idempotent
        }
        payment.setStatus(PaymentStatus.PAID);
        if (razorpayPaymentId != null) {
            payment.setRazorpayPaymentId(razorpayPaymentId);
        }
        confirmOrder(payment.getOrder(), message);
        log.info("Webhook confirmed payment for order {}", payment.getOrder().getId());
    }

    private void fail(Payment payment, String razorpayPaymentId) {
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.FAILED);
            if (razorpayPaymentId != null) {
                payment.setRazorpayPaymentId(razorpayPaymentId);
            }
            log.info("Webhook reported failed payment for order {}", payment.getOrder().getId());
        }
    }

    private void confirmOrder(Order order, String message) {
        if (order.getStatus() == OrderStatus.PLACED) {
            order.setStatus(OrderStatus.CONFIRMED);
        }
        orderRepository.save(order);
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("orderId", order.getId());
        payload.put("status", order.getStatus().name());
        payload.put("message", message);
        notificationService.publish(order.getUser().getEmail(), "ORDER_STATUS_CHANGED", payload);
    }

    private Order loadOwnableOrder(String email, Long orderId) {
        return orderRepository.findByIdAndUserEmailIgnoreCase(orderId, email)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private Payment requirePayment(Order order) {
        if (order.getPayment() == null) {
            throw new PaymentException("No payment record found for this order");
        }
        return order.getPayment();
    }

    private long toPaise(BigDecimal amount) {
        return amount.multiply(PAISE).longValueExact();
    }
}
