package com.sima.backend.controller;

import com.sima.backend.dto.request.RegistroTomaRequest;
import com.sima.backend.dto.response.ApiResponse;
import com.sima.backend.dto.response.RegistroTomaResponse;
import com.sima.backend.security.CustomUserDetails;
import com.sima.backend.service.RegistroTomaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HU-01: Tomas del día y recordatorios.
 * HU-02: Confirmar toma desde la app.
 * HU-13: Registro manual por cuidador.
 */
@RestController
@RequestMapping("/tomas")
@PreAuthorize("hasAnyRole('Familiar', 'Cuidador')")
public class RegistroTomaController {

    private final RegistroTomaService registroTomaService;

    public RegistroTomaController(RegistroTomaService registroTomaService) {
        this.registroTomaService = registroTomaService;
    }

    /**
     * GET /tomas/hoy/{idAdulto}
     * Retorna todas las tomas del día del adulto (base de HU-01 recordatorio).
     */
    @GetMapping("/hoy/{idAdulto}")
    public ResponseEntity<ApiResponse<List<RegistroTomaResponse>>> tomasDelDia(
            @PathVariable Integer idAdulto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.ok("Tomas del día obtenidas",
                        registroTomaService.listarTomasDelDia(
                                idAdulto, userDetails.getIdUsuario())));
    }

    /**
     * GET /tomas/proxima/{idAdulto}
     * Retorna la próxima toma pendiente del adulto (HU-04 chatbot).
     */
    @GetMapping("/proxima/{idAdulto}")
    public ResponseEntity<ApiResponse<RegistroTomaResponse>> proximaToma(
            @PathVariable Integer idAdulto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.ok("Próxima toma obtenida",
                        registroTomaService.obtenerProximaToma(
                                idAdulto, userDetails.getIdUsuario())));
    }

    /**
     * GET /tomas/historial/{idAdulto}
     * Retorna el historial de tomas en un rango de fechas (HU-11).
     */
    @GetMapping("/historial/{idAdulto}")
    public ResponseEntity<ApiResponse<List<RegistroTomaResponse>>> historial(
            @PathVariable Integer idAdulto,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.ok("Historial obtenido",
                        registroTomaService.listarHistorial(
                                idAdulto, userDetails.getIdUsuario(), desde, hasta)));
    }

    /**
     * POST /tomas/confirmar
     * Confirma que el adulto tomó su medicamento (HU-02 y HU-13).
     */
    @PostMapping("/confirmar")
    public ResponseEntity<ApiResponse<RegistroTomaResponse>> confirmar(
            @Valid @RequestBody RegistroTomaRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.ok("Toma confirmada exitosamente",
                        registroTomaService.confirmarToma(
                                request, userDetails.getIdUsuario())));
    }

    /**
     * POST /tomas/omitir
     * Registra una toma como omitida por el cuidador (HU-13).
     */
    @PostMapping("/omitir")
    @PreAuthorize("hasRole('Cuidador')")
    public ResponseEntity<ApiResponse<RegistroTomaResponse>> omitir(
            @Valid @RequestBody RegistroTomaRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.ok("Toma omitida exitosamente",
                        registroTomaService.omitirToma(
                                request, userDetails.getIdUsuario())));
    }

    /**
     * POST /tomas/revertir/{idRegistro}
     * Revierte una toma confirmada u omitida a estado pendiente (HU-13).
     */
    @PostMapping("/revertir/{idRegistro}")
    @PreAuthorize("hasRole('Cuidador')")
    public ResponseEntity<ApiResponse<RegistroTomaResponse>> revertir(
            @PathVariable Integer idRegistro,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.ok("Toma revertida a pendiente",
                        registroTomaService.revertirToma(
                                idRegistro, userDetails.getIdUsuario())));
    }
}