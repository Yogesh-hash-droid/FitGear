package com.fitgear.backend.notification;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Server-Sent Events hub for live order status updates.
 *
 * <p>One emitter per open browser connection, grouped by user email.
 * A scheduled heartbeat keeps connections alive; dead emitters are
 * removed on timeout/completion/error. Single-node by design - if the
 * app is ever scaled horizontally this is the piece that would move
 * behind a shared broker (e.g. Redis pub/sub).</p>
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final long EMITTER_TIMEOUT_MS = 300_000L; // 5 minutes, client reconnects
    private static final String ORDER_EVENT = "order-event";

    private final Map<String, Set<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "sse-heartbeat");
                thread.setDaemon(true);
                return thread;
            });

    public NotificationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeats, 25, 25, TimeUnit.SECONDS);
    }

    public SseEmitter subscribe(String email) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        String key = normalize(email);
        emittersByUser.computeIfAbsent(key, k -> new CopyOnWriteArraySet<>()).add(emitter);

        emitter.onCompletion(() -> remove(key, emitter));
        emitter.onTimeout(() -> remove(key, emitter));
        emitter.onError(e -> remove(key, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(json(Map.of("email", key, "timestamp", Instant.now().toString()))));
        } catch (IOException e) {
            remove(key, emitter);
        }
        log.debug("SSE stream opened for {}", key);
        return emitter;
    }

    /**
     * Push a structured event to every open stream of the given user.
     * Silently drops emitters that have gone stale.
     */
    public void publish(String email, String type, Object payload) {
        Set<SseEmitter> emitters = emittersByUser.get(normalize(email));
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        String body = json(Map.of(
                "type", type,
                "payload", payload == null ? Map.of() : payload,
                "timestamp", Instant.now().toString()
        ));
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(ORDER_EVENT).data(body));
            } catch (Exception e) {
                remove(normalize(email), emitter);
            }
        }
    }

    private void sendHeartbeats() {
        emittersByUser.forEach((email, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    remove(email, emitter);
                }
            }
        });
    }

    private void remove(String email, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersByUser.get(email);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByUser.remove(email, emitters);
            }
        }
    }

    private String json(Object value) {
        return objectMapper.writeValueAsString(value);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    @PreDestroy
    void shutdown() {
        heartbeatScheduler.shutdownNow();
        emittersByUser.values().forEach(Set::clear);
        emittersByUser.clear();
    }
}
