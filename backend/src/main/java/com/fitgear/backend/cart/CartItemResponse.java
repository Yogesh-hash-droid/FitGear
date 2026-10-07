package com.fitgear.backend.cart;

import com.fitgear.backend.product.ProductResponse;

public record CartItemResponse(
        Long id,
        ProductResponse product,
        Integer quantity
) {

    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(item.getId(), ProductResponse.from(item.getProduct()), item.getQuantity());
    }
}
