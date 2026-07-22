package com.sima.backend.controller;

import com.sima.backend.dto.request.LecturaPulseraRequest;
import com.sima.backend.dto.response.ApiResponse;
import com.sima.backend.dto.response.LecturaPulseraResponse;
import com.sima.backend.service.LecturaPulseraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para lecturas de pulsera inteligente.
 *
 * POST /lecturas-pulsera — Recibe lecturas desde la aplicación Android.
 * GET  /lecturas-pulsera/adulto/{id} — Historial de lecturas de un adulto.
 * GET  /lecturas-pulsera/adulto/{id}/ultima — Última lectura de un adulto.
 */
@RestController
@RequestMapping("/lecturas-pulsera")
public class LecturaPulseraController {

    private final LecturaPulseraService lecturaService;

    public LecturaPulseraController(LecturaPulseraService lecturaService) {
        this.lecturaService = lecturaService;
    }

    /**
     * POST /lecturas-pulsera
     * Recibe y guarda una lectura enviada desde la aplicación Android.
     * Acceso permitido sin JWT para la primera prueba local.
     * TODO: Proteger este endpoint antes de producción (API key, JWT para dispositivos, etc.)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LecturaPulseraResponse>> guardarLectura(
            @Valid @RequestBody LecturaPulseraRequest request) {

        LecturaPulseraResponse respuesta = lecturaService.guardarLectura(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Lectura guardada correctamente", respuesta));
    }

    /**
     * GET /lecturas-pulsera/adulto/{idAdulto}
     * Devuelve el historial completo de lecturas del adulto, de la más reciente a la más antigua.
     */
    @GetMapping("/adulto/{idAdulto}")
    @PreAuthorize("hasAnyRole('Administrador', 'Familiar', 'Cuidador')")
    public ResponseEntity<ApiResponse<List<LecturaPulseraResponse>>> obtenerHistorial(
            @PathVariable Integer idAdulto) {

        return ResponseEntity.ok(
                ApiResponse.ok("Historial de lecturas obtenido",
                        lecturaService.obtenerHistorialPorAdulto(idAdulto)));
    }

    /**
     * GET /lecturas-pulsera/adulto/{idAdulto}/ultima
     * Devuelve la lectura más reciente del adulto.
     */
    @GetMapping("/adulto/{idAdulto}/ultima")
    @PreAuthorize("hasAnyRole('Administrador', 'Familiar', 'Cuidador')")
    public ResponseEntity<ApiResponse<LecturaPulseraResponse>> obtenerUltimaLectura(
            @PathVariable Integer idAdulto) {

        return ResponseEntity.ok(
                ApiResponse.ok("Última lectura obtenida",
                        lecturaService.obtenerUltimaLectura(idAdulto)));
    }
}
