package com.sima.backend.controller;

import com.sima.backend.dto.request.CrearResenaRequest;
import com.sima.backend.dto.response.ApiResponse;
import com.sima.backend.dto.response.ResenaResponse;
import com.sima.backend.security.CustomUserDetails;
import com.sima.backend.service.ResenaService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/familiar/resenas")
public class FamiliarResenaController {

    private final ResenaService resenaService;

    public FamiliarResenaController(ResenaService resenaService) {
        this.resenaService = resenaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('Familiar')")
    public ApiResponse<ResenaResponse> crearResena(
            @Valid @RequestBody CrearResenaRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ResenaResponse resena = resenaService.crearResena(userDetails.getIdUsuario(), request);
        return ApiResponse.ok("Reseña creada exitosamente", resena);
    }
}
