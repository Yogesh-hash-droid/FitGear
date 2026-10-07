package com.fitgear.backend.cart;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productName, Integer requested, Integer available) {
        super("Insufficient stock for " + productName + ". Requested: " + requested + ", available: " + available);
    }
}
