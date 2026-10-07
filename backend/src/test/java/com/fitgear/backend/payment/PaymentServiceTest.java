package com.fitgear.backend.payment;

import com.fitgear.backend.auth.AppUser;
import com.fitgear.backend.auth.UserRole;
import com.fitgear.backend.notification.NotificationService;
import com.fitgear.backend.order.Order;
import com.fitgear.backend.order.OrderAddress;
import com.fitgear.backend.order.OrderRepository;
import com.fitgear.backend.order.OrderStatus;
import com.fitgear.backend.util.SignatureUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PaymentServiceTest {

    private static final String KEY_ID = "rzp_test_key";
    private static final String KEY_SECRET = "test_secret_value";

    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private NotificationService notificationService;
    private PaymentService service;

    private final AppUser user = new AppUser("Arun", "arun@example.com", "hashed", UserRole.CUSTOMER);

    @BeforeEach
    void setUp() {
        orderRepository = Mockito.mock(OrderRepository.class);
        paymentRepository = Mockito.mock(PaymentRepository.class);
        notificationService = Mockito.mock(NotificationService.class);
        service = new PaymentService(orderRepository, paymentRepository, notificationService,
                new ObjectMapper(), KEY_ID, KEY_SECRET, "whsec_test");
    }

    private Order orderWithPayment(PaymentStatus paymentStatus) {
        Order order = new Order(user, OrderStatus.PLACED, new BigDecimal("1000"), new BigDecimal("1000"),
                new OrderAddress("Arun", "9876543210", "12 Temple Street", null, "Chennai", "Tamil Nadu", "600001"));
        Payment payment = new Payment(order, PaymentMethod.RAZORPAY, paymentStatus, new BigDecimal("1000"));
        payment.setRazorpayOrderId("order_abc123");
        order.setPayment(payment);
        return order;
    }

    @Test
    void shouldConfirmOrderWhenSignatureIsValid() {
        Order order = orderWithPayment(PaymentStatus.PENDING);
        Mockito.when(orderRepository.findByIdAndUserEmailIgnoreCase(1L, "arun@example.com"))
                .thenReturn(Optional.of(order));
        Mockito.when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        String paymentId = "pay_xyz789";
        String signature = SignatureUtils.hmacSha256Hex(KEY_SECRET, "order_abc123|" + paymentId);

        var result = service.verifyPayment("arun@example.com",
                new VerifyPaymentRequest(1L, "order_abc123", paymentId, signature));

        assertEquals(OrderStatus.CONFIRMED, result.status());
        assertEquals(PaymentStatus.PAID, order.getPayment().getStatus());
        assertEquals(paymentId, order.getPayment().getRazorpayPaymentId());
        verify(notificationService).publish(anyString(), anyString(), any());
    }

    @Test
    void shouldRejectInvalidSignatureAndMarkPaymentFailed() {
        Order order = orderWithPayment(PaymentStatus.PENDING);
        Mockito.when(orderRepository.findByIdAndUserEmailIgnoreCase(1L, "arun@example.com"))
                .thenReturn(Optional.of(order));

        assertThrows(PaymentVerificationException.class, () -> service.verifyPayment("arun@example.com",
                new VerifyPaymentRequest(1L, "order_abc123", "pay_xyz789", "totally-wrong-signature")));

        assertEquals(PaymentStatus.FAILED, order.getPayment().getStatus());
        verify(notificationService, never()).publish(anyString(), anyString(), any());
    }

    @Test
    void shouldRejectWhenRazorpayOrderDoesNotMatch() {
        Order order = orderWithPayment(PaymentStatus.PENDING);
        Mockito.when(orderRepository.findByIdAndUserEmailIgnoreCase(1L, "arun@example.com"))
                .thenReturn(Optional.of(order));

        assertThrows(PaymentVerificationException.class, () -> service.verifyPayment("arun@example.com",
                new VerifyPaymentRequest(1L, "order_other", "pay_xyz789", "signature")));
    }

    @Test
    void shouldFailCheckoutWhenGatewayNotConfigured() {
        PaymentService unconfigured = new PaymentService(orderRepository, paymentRepository,
                notificationService, new ObjectMapper(), "", "", "");

        assertThrows(PaymentNotConfiguredException.class,
                () -> unconfigured.createCheckout("arun@example.com", 1L));
        assertEquals(false, unconfigured.isConfigured());
        assertEquals(false, unconfigured.config().razorpayEnabled());
    }

    @Test
    void shouldReportWhenRazorpayIsConfigured() {
        assertEquals(true, service.isConfigured());
        assertEquals(true, service.config().razorpayEnabled());
        assertEquals(KEY_ID, service.config().keyId());
    }

    @Test
    void webhookShouldConfirmOrderIdempotently() {
        Order order = orderWithPayment(PaymentStatus.PENDING);
        Payment payment = order.getPayment();
        Mockito.when(paymentRepository.findByRazorpayOrderId("order_abc123")).thenReturn(Optional.of(payment));
        Mockito.when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        String body = """
                {"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_1","order_id":"order_abc123"}}}}""";
        String signature = SignatureUtils.hmacSha256Hex("whsec_test", body);

        service.handleWebhook(body, signature);

        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());

        // second delivery of the same event must not fail or double-process
        service.handleWebhook(body, signature);
        assertEquals(PaymentStatus.PAID, payment.getStatus());
    }

    @Test
    void webhookShouldRejectInvalidSignature() {
        String body = "{\"event\":\"payment.captured\"}";

        assertThrows(PaymentVerificationException.class,
                () -> service.handleWebhook(body, "bad-signature"));
    }
}
