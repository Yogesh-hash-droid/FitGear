package com.fitgear.backend.payment;

public enum PaymentStatus {
    /** Payment initiated / awaiting customer action (COD stays pending until delivery). */
    PENDING,
    /** Payment captured and verified. */
    PAID,
    /** Payment attempt failed or signature verification failed. */
    FAILED,
    /** Order was cancelled after a successful payment. */
    REFUNDED,
    /** Order cancelled before payment completed. */
    CANCELLED
}
