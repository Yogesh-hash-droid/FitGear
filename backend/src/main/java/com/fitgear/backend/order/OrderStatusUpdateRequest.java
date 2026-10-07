package com.fitgear.backend.order;

import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull(message = "New status is required")
        OrderStatus status
) {
}
