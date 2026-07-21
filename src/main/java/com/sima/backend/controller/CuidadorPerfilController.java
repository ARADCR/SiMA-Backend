package com.sima.backend.controller;

import com.sima.backend.dto.request.ActualizarDatosContactoCuidadorRequest;
import com.sima.backend.dto.response.ApiResponse;
import com.sima.backend.dto.response.DatosContactoCuidadorResponse;
import com.sima.backend.security.CustomUserDetails;
import com.sima.backend.service.CuidadorPerfilService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Recurso self-service de datos de contacto/condiciones del Cuidador (HU-27).
// Separado de AiController (HU-21, análisis IA) y de UsuarioController (admin-only).
@RestController
@RequestMapping("/cuidador/perfil")
public class CuidadorPerfilController {

    private final CuidadorPerfilService cuidadorPerfilService;

    public CuidadorPerfilController(CuidadorPerfilService cuidadorPerfilService) {
        this.cuidadorPerfilService = cuidadorPerfilService;
    }

    // GET /api/cuidador/perfil -> datos de contacto/condiciones del cuidador autenticado
    @GetMapping
    @PreAuthorize("hasRole('Cuidador')")
    public ApiResponse<DatosContactoCuidadorResponse> obtenerPerfil(@AuthenticationPrincipal CustomUserDetails userDetails) {
        DatosContactoCuidadorResponse respuesta = cuidadorPerfilService.obtenerPerfil(userDetails.getIdUsuario());
        return ApiResponse.ok("Perfil obtenido", respuesta);
    }

    // PUT /api/cuidador/perfil -> edita correo, teléfono, ciudad, tarifa y disponibilidad
    @PutMapping
    @PreAuthorize("hasRole('Cuidador')")
    public ApiResponse<DatosContactoCuidadorResponse> actualizarPerfil(
            @Valid @RequestBody ActualizarDatosContactoCuidadorRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        DatosContactoCuidadorResponse respuesta = cuidadorPerfilService.actualizarPerfil(userDetails.getIdUsuario(), request);
        return ApiResponse.ok("Perfil actualizado", respuesta);
    }
}
