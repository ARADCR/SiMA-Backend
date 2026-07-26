package com.sima.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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
    private static final String TELEGRAM_DOC_API_URL = "https://api.telegram.org/bot{token}/sendDocument";

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

    @Async
    public void sendDocument(String chatId, byte[] fileData, String filename, String caption) {
        if (!enabled || "CONFIGURE_ME".equals(botToken)) {
            log.warn("Telegram bot is disabled or token is not configured. Document not sent to {}", chatId);
            return;
        }

        if (chatId == null || chatId.isBlank()) {
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("chat_id", chatId);
            if (caption != null && !caption.isEmpty()) {
                body.add("caption", caption);
            }
            
            ByteArrayResource resource = new ByteArrayResource(fileData) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            body.add("document", resource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            restTemplate.postForObject(
                    TELEGRAM_DOC_API_URL.replace("{token}", botToken),
                    requestEntity,
                    String.class
            );
            log.info("Documento {} enviado exitosamente a {}", filename, chatId);
        } catch (Exception e) {
            log.error("Error al enviar documento de Telegram al chat {}: {}", chatId, e.getMessage());
        }
    }
}
