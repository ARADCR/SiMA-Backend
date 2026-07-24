package com.sima.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalisisPerfilRequest {

    @NotBlank(message = "La descripción es requerida")
    @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
    private String descripcion;
}
