package com.sima.backend.controller;

import com.sima.backend.dto.request.MedicamentoRequest;
import com.sima.backend.dto.response.ApiResponse;
import com.sima.backend.dto.response.MedicamentoResponse;
import com.sima.backend.security.CustomUserDetails;
import com.sima.backend.service.MedicamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * HU-08: Agregar o editar la agenda de medicamentos del adulto mayor.
 * Familiar y Cuidador con validación de acceso por dato.
 */
@RestController
@RequestMapping("/medicamentos")
@PreAuthorize("hasAnyRole('Administrador', 'Familiar', 'Cuidador')")
public class MedicamentoController {

    private final MedicamentoService medicamentoService;

    public MedicamentoController(MedicamentoService medicamentoService) {
        this.medicamentoService = medicamentoService;
    }

    /**
     * GET /medicamentos/adulto/{idAdulto} — Listar medicamentos activos del adulto
     */
    @GetMapping("/adulto/{idAdulto}")
    public ResponseEntity<ApiResponse<List<MedicamentoResponse>>> listarPorAdulto(
            @PathVariable Integer idAdulto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.ok("Medicamentos obtenidos",
                        medicamentoService.listarActivosPorAdulto(
                                idAdulto, userDetails.getIdUsuario())));
    }

    /** POST /medicamentos — Crear medicamento con sus horarios */
    @PostMapping
    public ResponseEntity<ApiResponse<MedicamentoResponse>> crear(
            @Valid @RequestBody MedicamentoRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        MedicamentoResponse creado = medicamentoService.crear(request, userDetails.getIdUsuario());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Medicamento registrado exitosamente", creado));
    }

    /** PUT /medicamentos/{id} — Actualizar medicamento */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicamentoResponse>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody MedicamentoRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.ok("Medicamento actualizado",
                        medicamentoService.actualizar(id, request, userDetails.getIdUsuario())));
    }

    /** DELETE /medicamentos/{id} — Desactivar medicamento y sus horarios */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> desactivar(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        medicamentoService.desactivar(id, userDetails.getIdUsuario());
        return ResponseEntity.ok(
                ApiResponse.ok("Medicamento desactivado. El historial se conserva."));
    }
}