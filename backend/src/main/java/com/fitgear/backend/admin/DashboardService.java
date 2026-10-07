package com.fitgear.backend.admin;

import com.fitgear.backend.auth.UserRole;
import com.fitgear.backend.auth.UserRepository;
import com.fitgear.backend.order.OrderRepository;
import com.fitgear.backend.order.OrderResponse;
import com.fitgear.backend.order.OrderStatus;
import com.fitgear.backend.product.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardService {

    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int RECENT_ORDERS = 5;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public DashboardService(OrderRepository orderRepository,
                            ProductRepository productRepository,
                            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        Map<OrderStatus, Long> statusCounts = new LinkedHashMap<>();
        for (Object[] row : orderRepository.countByStatusGrouped()) {
            statusCounts.put((OrderStatus) row[0], (Long) row[1]);
        }

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            ordersByStatus.put(status.name(), statusCounts.getOrDefault(status, 0L));
        }

        BigDecimal revenue = orderRepository.sumTotalExcluding(OrderStatus.CANCELLED);

        return new DashboardResponse(
                orderRepository.count(),
                revenue == null ? BigDecimal.ZERO : revenue,
                ordersByStatus,
                productRepository.count(),
                productRepository.countByStockLessThanEqual(LOW_STOCK_THRESHOLD),
                userRepository.countByRole(UserRole.CUSTOMER),
                orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, RECENT_ORDERS))
                        .map(OrderResponse::from)
                        .getContent()
        );
    }
}
