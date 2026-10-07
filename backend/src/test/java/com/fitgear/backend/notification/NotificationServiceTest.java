package com.fitgear.backend.notification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationServiceTest {

    private NotificationService service = new NotificationService(new ObjectMapper());

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    void subscribeReturnsEmitterAndPublishDoesNotThrow() {
        SseEmitter emitter = assertDoesNotThrow(() -> service.subscribe("arun@example.com"));
        assertNotNull(emitter);

        // publish with no listeners must be a no-op, not an error
        assertDoesNotThrow(() -> service.publish("nobody@fitgear.test", "ORDER_STATUS_CHANGED",
                Map.of("orderId", 1, "status", "PLACED", "message", "hello")));

        // publish to an active subscriber must not throw either
        assertDoesNotThrow(() -> service.publish("ARUN@example.com", "ORDER_STATUS_CHANGED",
                Map.of("orderId", 42, "status", "SHIPPED", "message", "Order shipped")));
    }

    @Test
    void publishWithoutAnySubscribersIsSafe() {
        assertDoesNotThrow(() -> service.publish("ghost@fitgear.test", "ORDER_STATUS_CHANGED", Map.of()));
    }
}
