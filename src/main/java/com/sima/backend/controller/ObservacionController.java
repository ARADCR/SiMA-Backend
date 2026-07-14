package com.sima.backend.controller;

import com.sima.backend.dto.request.ObservacionRequest;
import com.sima.backend.dto.response.ApiResponse;
import com.sima.backend.dto.response.ObservacionResponse;
import com.sima.backend.security.CustomUserDetails;
import com.sima.backend.service.ObservacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/observaciones")
@PreAuthorize("hasAnyRole('Cuidador', 'Familiar', 'Administrador')")
public class ObservacionController {

    private final ObservacionService observacionService;

    public ObservacionController(ObservacionService observacionService) {
        this.observacionService = observacionService;
    }

    /** POST /observaciones — Registrar nota de observación (solo Cuidador) */
    @PostMapping
    @PreAuthorize("hasRole('Cuidador')")
    public ResponseEntity<ApiResponse<ObservacionResponse>> registrar(
            @Valid @RequestBody ObservacionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ObservacionResponse creada = observacionService.registrarObservacion(
                userDetails.getIdUsuario(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Observación registrada exitosamente", creada));
    }

    /** GET /observaciones/{idAdulto} — Listar observaciones de un adulto (Cuidador/Familiar/Administrador) */
    @GetMapping("/{idAdulto}")
    public ResponseEntity<ApiResponse<List<ObservacionResponse>>> listarPorAdulto(
            @PathVariable Integer idAdulto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<ObservacionResponse> observaciones = observacionService.listarPorAdulto(
                userDetails.getIdUsuario(), idAdulto);

        return ResponseEntity.ok(ApiResponse.ok("Observaciones obtenidas correctamente", observaciones));
    }
}
