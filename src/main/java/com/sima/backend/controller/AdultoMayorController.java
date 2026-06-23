package com.sima.backend.controller;

import com.sima.backend.dto.request.AdultoMayorRequest;
import com.sima.backend.dto.response.AdultoMayorResponse;
import com.sima.backend.dto.response.ApiResponse;
import com.sima.backend.security.CustomUserDetails;
import com.sima.backend.service.AdultoMayorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para gestionar la información de los adultos mayores.
 * Implementación para HU-12: Registrar o actualizar datos personales del adulto mayor.
 */
@RestController
@RequestMapping("/adultos")
@PreAuthorize("hasAnyRole('Administrador', 'Familiar', 'Cuidador', 'Adulto Mayor')")
public class AdultoMayorController {

    private final AdultoMayorService adultoService;

    public AdultoMayorController(AdultoMayorService adultoService) {
        this.adultoService = adultoService;
    }

    /** GET /adultos — Listar adultos asignados al usuario autenticado */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdultoMayorResponse>>> listar(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.ok("Adultos obtenidos",
                        adultoService.listarPorUsuario(userDetails.getIdUsuario())));
    }

    /**
     * GET /adultos/mi-perfil — El adulto mayor obtiene su propio idAdulto.
     * Busca en relacion_usuario_adulto la primera relación del usuario autenticado.
     */
    @GetMapping("/mi-perfil")
    @PreAuthorize("hasRole('Adulto Mayor')")
    public ResponseEntity<ApiResponse<AdultoMayorResponse>> miPerfil(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<AdultoMayorResponse> mis = adultoService.listarPorUsuario(userDetails.getIdUsuario());
        if (mis.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("No tienes un perfil de adulto mayor vinculado", null));
        }
        return ResponseEntity.ok(ApiResponse.ok("Perfil obtenido", mis.get(0)));
    }
    /** GET /adultos/{id} — Obtener un adulto por ID */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdultoMayorResponse>> obtener(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.ok("Adulto obtenido",
                        adultoService.buscarPorId(id, userDetails.getIdUsuario())));
    }

    /** POST /adultos — Registrar nuevo adulto mayor */
    @PostMapping
    @PreAuthorize("hasAnyRole('Administrador', 'Familiar')")
    public ResponseEntity<ApiResponse<AdultoMayorResponse>> registrar(
            @Valid @RequestBody AdultoMayorRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        AdultoMayorResponse creado = adultoService.registrar(request, userDetails.getIdUsuario());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Adulto mayor registrado exitosamente", creado));
    }

    /** PUT /adultos/{id} — Actualizar datos del adulto mayor */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdultoMayorResponse>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody AdultoMayorRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.ok("Adulto mayor actualizado",
                        adultoService.actualizar(id, request, userDetails.getIdUsuario())));
    }
}