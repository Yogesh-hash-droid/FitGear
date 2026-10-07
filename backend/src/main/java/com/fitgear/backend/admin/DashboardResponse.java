package com.fitgear.backend.admin;

import com.fitgear.backend.order.OrderResponse;
import com.fitgear.backend.order.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
        long totalOrders,
        BigDecimal totalRevenue,
        Map<String, Long> ordersByStatus,
        long totalProducts,
        long lowStockProducts,
        long totalCustomers,
        List<OrderResponse> recentOrders
) {
}
