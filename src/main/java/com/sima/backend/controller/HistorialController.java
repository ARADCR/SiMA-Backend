package com.sima.backend.controller;

import com.sima.backend.dto.response.ApiResponse;
import com.sima.backend.dto.response.HistorialEventoResponse;
import com.sima.backend.security.CustomUserDetails;
import com.sima.backend.service.HistorialService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/historial")
@PreAuthorize("hasAnyRole('Familiar', 'Cuidador', 'Administrador')")
public class HistorialController {

    private final HistorialService historialService;

    public HistorialController(HistorialService historialService) {
        this.historialService = historialService;
    }

    @GetMapping("/{idAdulto}")
    public ResponseEntity<ApiResponse<Page<HistorialEventoResponse>>> obtenerHistorial(
            @PathVariable Integer idAdulto,
            @RequestParam(required = false) String tipoEvento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Pageable pageable = PageRequest.of(page, size);
        Page<HistorialEventoResponse> result = historialService.obtenerHistorial(
                userDetails.getIdUsuario(),
                idAdulto,
                tipoEvento,
                fechaInicio,
                fechaFin,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.ok("Historial de salud obtenido correctamente", result));
    }
}
