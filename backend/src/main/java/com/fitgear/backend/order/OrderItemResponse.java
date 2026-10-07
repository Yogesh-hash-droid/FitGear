package com.fitgear.backend.order;

import java.math.BigDecimal;

public record OrderItemResponse(Long productId, String productName, BigDecimal unitPrice, Integer quantity) {}
