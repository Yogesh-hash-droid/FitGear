package com.fitgear.backend.payment;

import com.fitgear.backend.order.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** Lets the checkout page decide whether to offer Razorpay or COD only. */
    @GetMapping("/config")
    public PaymentConfigResponse getConfig() {
        return paymentService.config();
    }

    @PostMapping("/checkout")
    public CheckoutResponse createCheckout(
            Authentication authentication,
            @Valid @RequestBody CheckoutRequest request
    ) {
        return paymentService.createCheckout(authentication.getName(), request.orderId());
    }

    /** Server-side signature verification - the frontend result alone is never trusted. */
    @PostMapping("/verify")
    public OrderResponse verify(
            Authentication authentication,
            @Valid @RequestBody VerifyPaymentRequest request
    ) {
        return paymentService.verifyPayment(authentication.getName(), request);
    }

    /** Razorpay webhook (public endpoint, authenticated by HMAC signature). */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> webhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {
        paymentService.handleWebhook(rawBody, signature == null ? "" : signature);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
