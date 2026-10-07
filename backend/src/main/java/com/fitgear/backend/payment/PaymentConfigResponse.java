package com.fitgear.backend.payment;

/**
 * Whether Razorpay is configured - lets the checkout page hide the
 * online-payment option when keys are absent (COD-only mode).
 */
public record PaymentConfigResponse(
        boolean razorpayEnabled,
        String keyId
) {
}
