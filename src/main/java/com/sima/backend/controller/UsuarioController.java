package com.sima.backend.controller;

import com.sima.backend.dto.request.UsuarioCreateRequest;
import com.sima.backend.dto.request.UsuarioUpdateRequest;
import com.sima.backend.dto.response.ApiResponse;
import com.sima.backend.dto.response.UsuarioResponse;
import com.sima.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * HU-17: Crear, editar y desactivar cuentas de familiares/cuidadores.
 * Solo accesible por el rol Administrador.
 */
/**
 * Controlador para la gestión de usuarios del sistema.
 * Implementación para HU-17: Crear, editar o eliminar cuentas de
 * familiares/cuidadores.
 */
@RestController
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('Administrador')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** GET /usuarios — Listar todos los usuarios */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> listar() {
        return ResponseEntity.ok(
                ApiResponse.ok("Usuarios obtenidos", usuarioService.listarTodos()));
    }

    /** GET /usuarios/{id} — Obtener un usuario por ID */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Usuario obtenido", usuarioService.buscarPorId(id)));
    }

    /** POST /usuarios — Crear nuevo usuario */
    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioResponse>> crear(
            @Valid @RequestBody UsuarioCreateRequest request) {

        UsuarioResponse creado = usuarioService.crear(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Usuario creado exitosamente", creado));
    }

    /** PUT /usuarios/{id} — Actualizar usuario */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody UsuarioUpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Usuario actualizado", usuarioService.actualizar(id, request)));
    }

    /** DELETE /usuarios/{id} — Desactivar usuario (soft delete) */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable Integer id) {
        usuarioService.desactivar(id);
        return ResponseEntity.ok(ApiResponse.ok("Usuario desactivado exitosamente"));
    }

    /** PATCH /usuarios/{id}/activar — Reactivar usuario */
    @PatchMapping("/{id}/activar")
    public ResponseEntity<ApiResponse<Void>> activar(@PathVariable Integer id) {
        usuarioService.activar(id);
        return ResponseEntity.ok(ApiResponse.ok("Usuario activado exitosamente"));
    }

    /** PATCH /usuarios/{id}/desactivar — Desactivar usuario (alias explícito) */
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<ApiResponse<Void>> desactivarPatch(@PathVariable Integer id) {
        usuarioService.desactivar(id);
        return ResponseEntity.ok(ApiResponse.ok("Usuario desactivado exitosamente"));
    }
}