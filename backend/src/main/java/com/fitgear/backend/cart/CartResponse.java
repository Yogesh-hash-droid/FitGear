package com.fitgear.backend.cart;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full cart view. subtotal/itemCount are always computed on the server
 * from current product prices - never accepted from the client.
 */
public record CartResponse(
        List<CartItemResponse> items,
        int itemCount,
        BigDecimal subtotal
) {

    public static CartResponse of(List<CartItemResponse> items) {
        int itemCount = 0;
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItemResponse item : items) {
            itemCount += item.quantity();
            subtotal = subtotal.add(item.product().price().multiply(BigDecimal.valueOf(item.quantity())));
        }
        return new CartResponse(items, itemCount, subtotal);
    }
}
