package com.fitgear.backend.order;

import com.fitgear.backend.payment.PaymentMethod;
import com.fitgear.backend.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        String customerName,
        String customerEmail,
        BigDecimal subtotal,
        BigDecimal total,
        AddressResponse address,
        PaymentInfo payment,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt,
        boolean cancelEligible
) {

    public record AddressResponse(
            String receiverName,
            String phone,
            String line1,
            String line2,
            String city,
            String state,
            String pincode
    ) {
        public static AddressResponse from(OrderAddress address) {
            if (address == null) {
                return null;
            }
            return new AddressResponse(
                    address.getReceiverName(),
                    address.getPhone(),
                    address.getLine1(),
                    address.getLine2(),
                    address.getCity(),
                    address.getState(),
                    address.getPincode()
            );
        }
    }

    public record PaymentInfo(
            PaymentMethod method,
            PaymentStatus status,
            String razorpayOrderId,
            String razorpayPaymentId
    ) {
        public static PaymentInfo from(com.fitgear.backend.payment.Payment payment) {
            if (payment == null) {
                return null;
            }
            return new PaymentInfo(
                    payment.getMethod(),
                    payment.getStatus(),
                    payment.getRazorpayOrderId(),
                    payment.getRazorpayPaymentId()
            );
        }
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getUser().getName(),
                order.getUser().getEmail(),
                order.getSubtotal(),
                order.getTotal(),
                AddressResponse.from(order.getAddress()),
                PaymentInfo.from(order.getPayment()),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                isCancelEligible(order.getStatus())
        );
    }

    public static boolean isCancelEligible(OrderStatus status) {
        return status == OrderStatus.PLACED
                || status == OrderStatus.CONFIRMED
                || status == OrderStatus.PROCESSING;
    }
}
