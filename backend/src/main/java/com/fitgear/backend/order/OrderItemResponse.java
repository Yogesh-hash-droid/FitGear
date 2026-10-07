package com.fitgear.backend.order;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String productName,
        String imageUrl,
        BigDecimal unitPrice,
        Integer quantity
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getImageUrl(),
                item.getUnitPrice(),
                item.getQuantity()
        );
    }
}
