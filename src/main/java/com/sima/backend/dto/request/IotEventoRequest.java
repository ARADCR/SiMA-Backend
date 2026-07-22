package com.sima.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IotEventoRequest {
    @NotBlank(message = "La MAC (identificador físico) es obligatoria")
    private String mac;
    
    @NotBlank(message = "El tipo de evento es obligatorio")
    private String tipoEvento;
    
    private String detalle;
    
    // Opcional: El ID del registro de toma que se está confirmando
    private Integer idRegistro;
}
