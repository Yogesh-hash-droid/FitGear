package com.fitgear.backend.payment;

/**
 * Everything the React client needs to open the Razorpay checkout widget.
 * Amount is in paise (INR minor units) as required by Razorpay.
 */
public record CheckoutResponse(
        String keyId,
        String razorpayOrderId,
        long amountPaise,
        String currency,
        Long orderId,
        String customerName,
        String customerEmail
) {
}
