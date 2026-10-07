package com.fitgear.backend.payment;

/**
 * Payment-related business rule violation (mapped to HTTP 409).
 */
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }
}
