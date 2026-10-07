package com.fitgear.backend.payment;

/**
 * Razorpay signature verification failed (mapped to HTTP 400).
 */
public class PaymentVerificationException extends RuntimeException {

    public PaymentVerificationException(String message) {
        super(message);
    }
}
