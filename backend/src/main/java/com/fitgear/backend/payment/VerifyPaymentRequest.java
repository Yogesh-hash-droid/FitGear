package com.fitgear.backend.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Fields returned by the Razorpay checkout widget to the browser.
 * Verified server-side - never trusted as-is.
 */
public record VerifyPaymentRequest(
        @NotNull(message = "Order id is required") Long orderId,
        @NotBlank(message = "Razorpay order id is required") String razorpayOrderId,
        @NotBlank(message = "Razorpay payment id is required") String razorpayPaymentId,
        @NotBlank(message = "Razorpay signature is required") String razorpaySignature
) {
}
