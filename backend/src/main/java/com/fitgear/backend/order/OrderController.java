package com.fitgear.backend.order;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) { this.orderService = orderService; }

    @PostMapping
    public org.springframework.http.ResponseEntity<OrderResponse> placeOrder(Authentication authentication) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(authentication.getName()));
    }

    @GetMapping
    public List<OrderResponse> getOrders(Authentication authentication) {
        return orderService.getOrders(authentication.getName());
    }

    @PatchMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(Authentication authentication, @PathVariable Long orderId) {
        return orderService.cancelOrder(authentication.getName(), orderId);
    }
}
