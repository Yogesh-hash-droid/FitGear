package com.fitgear.backend.payment;

/**
 * Razorpay credentials are not configured (mapped to HTTP 503).
 * The storefront then offers Cash on Delivery only.
 */
public class PaymentNotConfiguredException extends RuntimeException {

    public PaymentNotConfiguredException() {
        super("Online payments are not configured. Please choose Cash on Delivery.");
    }
}
