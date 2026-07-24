package com.sima.backend.controller;

import com.sima.backend.dto.request.SolicitudVinculacionRequest;
import com.sima.backend.dto.response.ApiResponse;
import com.sima.backend.dto.response.CuidadorPublicResponse;
import com.sima.backend.dto.response.SolicitudVinculacionResponse;
import com.sima.backend.security.CustomUserDetails;
import com.sima.backend.service.VinculacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vinculaciones")
public class VinculacionController {

    private final VinculacionService vinculacionService;

    public VinculacionController(VinculacionService vinculacionService) {
        this.vinculacionService = vinculacionService;
    }

    /** GET /vinculaciones/cuidadores — Listar cuidadores disponibles para búsqueda */
    @GetMapping("/cuidadores")
    @PreAuthorize("hasRole('Familiar')")
    public ResponseEntity<ApiResponse<List<CuidadorPublicResponse>>> listarCuidadores() {
        return ResponseEntity.ok(
                ApiResponse.ok("Cuidadores disponibles", vinculacionService.listarCuidadoresDisponibles()));
    }

    /** POST /vinculaciones/solicitar — Familiar envía solicitud a un cuidador */
    @PostMapping("/solicitar")
    @PreAuthorize("hasRole('Familiar')")
    public ResponseEntity<ApiResponse<SolicitudVinculacionResponse>> solicitar(
            @Valid @RequestBody SolicitudVinculacionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        SolicitudVinculacionResponse response = vinculacionService.enviarSolicitud(request, userDetails.getIdUsuario());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Solicitud enviada exitosamente", response));
    }

    /** GET /vinculaciones/pendientes — Cuidador revisa sus solicitudes entrantes */
    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('Cuidador')")
    public ResponseEntity<ApiResponse<List<SolicitudVinculacionResponse>>> listarPendientes(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        return ResponseEntity.ok(
                ApiResponse.ok("Solicitudes pendientes", vinculacionService.listarSolicitudesPendientes(userDetails.getIdUsuario())));
    }

    /** POST /vinculaciones/{idSolicitud}/responder?aceptar=true/false — Cuidador responde */
    @PostMapping("/{idSolicitud}/responder")
    @PreAuthorize("hasRole('Cuidador')")
    public ResponseEntity<ApiResponse<SolicitudVinculacionResponse>> responder(
            @PathVariable Integer idSolicitud,
            @RequestParam boolean aceptar,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        SolicitudVinculacionResponse response = vinculacionService.responderSolicitud(idSolicitud, aceptar, userDetails.getIdUsuario());
        return ResponseEntity.ok(
                ApiResponse.ok("Solicitud " + (aceptar ? "aceptada" : "rechazada") + " exitosamente", response));
    }
}
