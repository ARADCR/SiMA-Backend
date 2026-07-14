package com.sima.backend.controller;

import com.sima.backend.dto.request.AsignarDispositivoRequest;
import com.sima.backend.dto.request.DispositivoIotRequest;
import com.sima.backend.dto.response.ApiResponse;
import com.sima.backend.dto.response.DispositivoIotResponse;
import com.sima.backend.service.DispositivoIotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * HU-18: Registrar y asignar dispositivos IoT a un adulto mayor.
 */
@RestController
@RequestMapping("/dispositivos")
public class DispositivoIotController {

    private final DispositivoIotService dispositivoService;

    public DispositivoIotController(DispositivoIotService dispositivoService) {
        this.dispositivoService = dispositivoService;
    }

    /** GET /dispositivos — Listar todos los dispositivos activos */
    @GetMapping
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<ApiResponse<List<DispositivoIotResponse>>> listar() {
        return ResponseEntity.ok(
                ApiResponse.ok("Dispositivos obtenidos", dispositivoService.listarTodos()));
    }

    /** GET /dispositivos/sin-asignar — Dispositivos sin adulto asignado */
    @GetMapping("/sin-asignar")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<ApiResponse<List<DispositivoIotResponse>>> listarSinAsignar() {
        return ResponseEntity.ok(
                ApiResponse.ok("Dispositivos sin asignar", dispositivoService.listarSinAsignar()));
    }

    /** GET /dispositivos/adulto/{idAdulto} — Dispositivos de un adulto */
    @GetMapping("/adulto/{idAdulto}")
    @PreAuthorize("hasAnyRole('Administrador', 'Familiar', 'Cuidador')")
    public ResponseEntity<ApiResponse<List<DispositivoIotResponse>>> listarPorAdulto(
            @PathVariable Integer idAdulto) {

        return ResponseEntity.ok(
                ApiResponse.ok("Dispositivos del adulto",
                        dispositivoService.listarPorAdulto(idAdulto)));
    }

    /** POST /dispositivos — Registrar nuevo dispositivo */
    @PostMapping
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<ApiResponse<DispositivoIotResponse>> registrar(
            @Valid @RequestBody DispositivoIotRequest request) {

        DispositivoIotResponse creado = dispositivoService.registrar(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Dispositivo registrado exitosamente", creado));
    }

    /** PUT /dispositivos/{id}/asignar — Asignar dispositivo a un adulto */
    @PutMapping("/{id}/asignar")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<ApiResponse<DispositivoIotResponse>> asignar(
            @PathVariable Integer id,
            @Valid @RequestBody AsignarDispositivoRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Dispositivo asignado exitosamente",
                        dispositivoService.asignar(id, request)));
    }

    /** PUT /dispositivos/{id}/desasignar — Desasignar dispositivo de su adulto */
    @PutMapping("/{id}/desasignar")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<ApiResponse<DispositivoIotResponse>> desasignar(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                ApiResponse.ok("Dispositivo desasignado exitosamente",
                        dispositivoService.desasignar(id)));
    }

    /** PUT /dispositivos/{id} — Actualizar datos del dispositivo */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<ApiResponse<DispositivoIotResponse>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody DispositivoIotRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Dispositivo actualizado exitosamente",
                        dispositivoService.actualizar(id, request)));
    }
}