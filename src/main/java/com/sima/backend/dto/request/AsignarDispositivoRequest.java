package com.sima.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para asignar un dispositivo ya registrado a un adulto mayor.
 * HU-18: operación separada del registro del dispositivo.
 */
@Getter
@Setter
@NoArgsConstructor
public class AsignarDispositivoRequest {

    @NotNull(message = "El ID del adulto mayor es obligatorio")
    private Integer idAdulto;
}