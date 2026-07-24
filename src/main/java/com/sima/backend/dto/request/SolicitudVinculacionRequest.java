package com.sima.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SolicitudVinculacionRequest {
    @NotNull(message = "El ID del adulto es obligatorio")
    private Integer idAdulto;

    @NotNull(message = "El ID del cuidador es obligatorio")
    private Integer idCuidador;
}
