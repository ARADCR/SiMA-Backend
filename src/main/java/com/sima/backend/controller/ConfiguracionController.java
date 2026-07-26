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
import com.sima.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/configuracion")
public class ConfiguracionController {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionController.class);

    @Value("${telegram.bot.token}")
    private String botToken;

    private final RestTemplate restTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private com.sima.backend.repository.RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository;

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
            return ResponseEntity.badRequest().body(ApiResponse.error("Error de Telegram (Asegurate de haberle dado a /start al bot primero): " + e.getMessage()));
        }
    }

    @PostMapping("/telegram/save")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<ApiResponse<Void>> guardarTelegram(@RequestBody Map<String, String> request, @org.springframework.security.core.annotation.AuthenticationPrincipal com.sima.backend.security.CustomUserDetails userDetails) {
        String chatId = request.get("chatId");
        
        com.sima.backend.entity.Usuario adminUser = usuarioRepository.findById(userDetails.getIdUsuario()).orElse(null);
        if (adminUser != null) {
            adminUser.setTelegramChatId(chatId);
            usuarioRepository.save(adminUser);
            return ResponseEntity.ok(ApiResponse.ok("Chat ID guardado exitosamente", null));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("No se pudo guardar el Chat ID"));
    }

    @GetMapping("/telegram/suscripciones")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<ApiResponse<java.util.List<com.sima.backend.dto.response.SuscripcionTelegramResponse>>> getSuscripciones() {
        java.util.List<com.sima.backend.entity.RelacionUsuarioAdulto> relaciones = relacionUsuarioAdultoRepository.findAll();
        
        java.util.List<com.sima.backend.dto.response.SuscripcionTelegramResponse> suscripciones = relaciones.stream()
            .map(r -> {
                com.sima.backend.entity.Usuario u = r.getUsuario();
                com.sima.backend.entity.AdultoMayor a = r.getAdulto();
                boolean habilitado = u.getTelegramChatId() != null && !u.getTelegramChatId().isBlank();
                
                return com.sima.backend.dto.response.SuscripcionTelegramResponse.builder()
                    .id(u.getIdUsuario() + "-" + a.getIdAdulto())
                    .familiar(u.getNombre() + " " + u.getApellido())
                    .adulto(a.getNombre() + " " + a.getApellido())
                    .habilitado(habilitado)
                    .ultimaNotif("Sin datos")
                    .build();
            })
            .collect(java.util.stream.Collectors.toList());
            
        return ResponseEntity.ok(ApiResponse.ok("Suscripciones obtenidas", suscripciones));
    }
}
