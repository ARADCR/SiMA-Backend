package com.sima.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para enviar notificaciones push usando la API de Telegram Bot.
 * Opera de forma asíncrona para no bloquear el hilo principal.
 */
@Service
public class TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot{token}/sendMessage";

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate;

    public TelegramService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Async
    public void sendMessage(String chatId, String message) {
        if (!enabled || "CONFIGURE_ME".equals(botToken)) {
            log.warn("Telegram bot is disabled or token is not configured. Message not sent to {}", chatId);
            return;
        }

        if (chatId == null || chatId.isBlank()) {
            return;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("chat_id", chatId);
            payload.put("text", message);

            restTemplate.postForObject(
                    TELEGRAM_API_URL.replace("{token}", botToken),
                    payload,
                    String.class
            );
            log.info("Mensaje de Telegram enviado exitosamente a {}", chatId);
        } catch (Exception e) {
            log.error("Error al enviar mensaje de Telegram al chat {}: {}", chatId, e.getMessage());
        }
    }
}
