package com.sima.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    
    // Mapa que asocia el idUsuario (Familiar/Cuidador) con su SseEmitter
    private final Map<Integer, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Integer idUsuario) {
        // Timeout configurado a 0 (infinito) o algo muy largo
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        emitters.put(idUsuario, emitter);
        log.info("Nuevo cliente suscrito para notificaciones: idUsuario={}", idUsuario);

        emitter.onCompletion(() -> {
            log.info("SseEmitter completado para idUsuario={}", idUsuario);
            emitters.remove(idUsuario);
        });

        emitter.onTimeout(() -> {
            log.warn("SseEmitter timeout para idUsuario={}", idUsuario);
            emitter.complete();
            emitters.remove(idUsuario);
        });

        emitter.onError(e -> {
            log.error("SseEmitter error para idUsuario={}", idUsuario, e);
            emitter.completeWithError(e);
            emitters.remove(idUsuario);
        });

        // Enviar un evento inicial para verificar conexión
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Conexión establecida para notificaciones"));
        } catch (IOException e) {
            log.error("Error al enviar evento INIT para idUsuario={}", idUsuario, e);
            emitter.completeWithError(e);
            emitters.remove(idUsuario);
        }

        return emitter;
    }

    public void sendNotification(Integer idUsuario, String eventName, Object data) {
        SseEmitter emitter = emitters.get(idUsuario);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
                log.info("Notificación '{}' enviada exitosamente a idUsuario={}", eventName, idUsuario);
            } catch (IOException e) {
                log.error("Error al enviar notificación '{}' a idUsuario={}, eliminando emisor", eventName, idUsuario, e);
                emitter.completeWithError(e);
                emitters.remove(idUsuario);
            }
        } else {
            log.debug("No hay cliente conectado para idUsuario={}. La notificación no se entregó en tiempo real.", idUsuario);
        }
    }
}
