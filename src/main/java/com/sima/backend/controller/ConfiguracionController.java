package com.sima.backend.controller;

import com.sima.backend.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/configuracion")
public class ConfiguracionController {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionController.class);

    @Value("${telegram.bot.token}")
    private String botToken;

    private final RestTemplate restTemplate;

    public ConfiguracionController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/telegram/test")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<ApiResponse<String>> probarTelegram(@RequestBody Map<String, String> request) {
        String chatId = request.get("chatId");
        
        if (chatId == null || chatId.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Se requiere el chatId para probar la conexión"));
        }

        if ("CONFIGURE_ME".equals(botToken)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("El token de Telegram no está configurado en el backend"));
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("chat_id", chatId);
            payload.put("text", "✅ ¡Conexión exitosa! SiMA está configurado correctamente con Telegram.");

            restTemplate.postForObject(
                    "https://api.telegram.org/bot{token}/sendMessage",
                    payload,
                    String.class,
                    botToken
            );
            return ResponseEntity.ok(ApiResponse.ok("Mensaje de prueba enviado exitosamente", null));
        } catch (Exception e) {
            log.error("Error al probar conexión de Telegram", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error al enviar mensaje de prueba: " + e.getMessage()));
        }
    }
}
