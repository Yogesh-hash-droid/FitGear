package com.fitgear.backend.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        BigDecimal total,
        OrderStatus status,
        Instant createdAt,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getTotal(), order.getStatus(), order.getCreatedAt(),
                order.getItems().stream().map(item -> new OrderItemResponse(
                        item.getProductId(), item.getProductName(), item.getUnitPrice(), item.getQuantity()
                )).toList());
    }
}
