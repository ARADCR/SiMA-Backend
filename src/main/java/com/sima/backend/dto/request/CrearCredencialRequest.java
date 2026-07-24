package com.sima.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearCredencialRequest {
    
    @NotBlank(message = "El tipo de documento es obligatorio")
    private String tipo;

    @NotBlank(message = "El nombre del documento es obligatorio")
    private String nombre;

    private String archivoFalsoNombre;
}
