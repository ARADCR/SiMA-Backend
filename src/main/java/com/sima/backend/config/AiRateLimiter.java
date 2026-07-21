package com.sima.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter en memoria: ventana deslizante de 1 minuto por usuario.
 * No usa Redis para no complicar el stack (ver plan HU-19).
 */
@Component
public class AiRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Value("${sima.ai.rate-limit.requests-per-minute:20}")
    private int requestsPerMinute;

    private final Map<Integer, Deque<Instant>> requestsByUser = new ConcurrentHashMap<>();

    public synchronized boolean allowRequest(Integer idUsuario) {
        Instant now = Instant.now();
        Deque<Instant> timestamps = requestsByUser.computeIfAbsent(idUsuario, k -> new ArrayDeque<>());

        while (!timestamps.isEmpty() && Duration.between(timestamps.peekFirst(), now).compareTo(WINDOW) > 0) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= requestsPerMinute) {
            return false;
        }

        timestamps.addLast(now);
        return true;
    }
}
