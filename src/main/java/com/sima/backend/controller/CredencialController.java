package com.sima.backend.controller;

import com.sima.backend.dto.request.CrearCredencialRequest;
import com.sima.backend.dto.response.ApiResponse;
import com.sima.backend.dto.response.CredencialResponse;
import com.sima.backend.security.CustomUserDetails;
import com.sima.backend.service.CredencialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cuidador/credenciales")
@RequiredArgsConstructor
public class CredencialController {

    private final CredencialService credencialService;

    @GetMapping
    @PreAuthorize("hasRole('Cuidador')")
    public ApiResponse<List<CredencialResponse>> obtenerCredenciales(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<CredencialResponse> credenciales = credencialService.obtenerCredencialesPorCuidador(userDetails.getIdUsuario());
        return ApiResponse.ok("Credenciales obtenidas exitosamente", credenciales);
    }

    @PostMapping
    @PreAuthorize("hasRole('Cuidador')")
    public ApiResponse<CredencialResponse> crearCredencial(
            @Valid @RequestBody CrearCredencialRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CredencialResponse nueva = credencialService.crearCredencial(userDetails.getIdUsuario(), request);
        return ApiResponse.ok("Credencial subida exitosamente", nueva);
    }
}
