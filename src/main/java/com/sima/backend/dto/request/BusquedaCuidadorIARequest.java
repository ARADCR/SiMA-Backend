package com.sima.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusquedaCuidadorIARequest {

    @NotBlank(message = "La búsqueda es requerida")
    private String query;

    @NotNull(message = "El adulto mayor es requerido")
    private Integer idAdulto;
}
