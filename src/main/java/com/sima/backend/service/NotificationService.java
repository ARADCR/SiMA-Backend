package com.sima.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conecta el canal de notificación (app) al servicio de recordatorios.
 * Implementación para HU-01: Recibir recordatorio de medicamento.
 */
@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    
    // Mapa que asocia el id_adulto con su respectivo SseEmitter
    private final Map<Integer, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Integer idAdulto) {
        // Timeout configurado a 0 (infinito) o algo muy largo
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        emitters.put(idAdulto, emitter);
        log.info("Nuevo cliente suscrito para notificaciones: idAdulto={}", idAdulto);

        emitter.onCompletion(() -> {
            log.info("SseEmitter completado para idAdulto={}", idAdulto);
            emitters.remove(idAdulto);
        });

        emitter.onTimeout(() -> {
            log.warn("SseEmitter timeout para idAdulto={}", idAdulto);
            emitter.complete();
            emitters.remove(idAdulto);
        });

        emitter.onError(e -> {
            log.error("SseEmitter error para idAdulto={}", idAdulto, e);
            emitter.completeWithError(e);
            emitters.remove(idAdulto);
        });

        // Enviar un evento inicial para verificar conexión
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Conexión establecida para notificaciones"));
        } catch (IOException e) {
            log.error("Error al enviar evento INIT para idAdulto={}", idAdulto, e);
            emitter.completeWithError(e);
            emitters.remove(idAdulto);
        }

        return emitter;
    }

    public void sendNotification(Integer idAdulto, String eventName, Object data) {
        SseEmitter emitter = emitters.get(idAdulto);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
                log.info("Notificación '{}' enviada exitosamente a idAdulto={}", eventName, idAdulto);
            } catch (IOException e) {
                log.error("Error al enviar notificación '{}' a idAdulto={}, eliminando emisor", eventName, idAdulto, e);
                emitter.completeWithError(e);
                emitters.remove(idAdulto);
            }
        } else {
            log.debug("No hay cliente conectado para idAdulto={}. La notificación no se entregó en tiempo real.", idAdulto);
        }
    }
}
