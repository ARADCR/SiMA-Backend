package com.sima.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ObservacionRequest {

    @NotNull(message = "El id del adulto mayor es obligatorio")
    private Integer idAdulto;

    @NotBlank(message = "La urgencia es obligatoria")
    @Pattern(regexp = "normal|importante|urgente", message = "La urgencia debe ser 'normal', 'importante' o 'urgente'")
    private String urgencia;

    @NotBlank(message = "El texto de la observación es obligatorio")
    @Size(max = 1000, message = "El texto no puede superar 1000 caracteres")
    private String texto;

    private String tensionArterial;

    private String frecuenciaCardiaca;

    private String temperatura;
}
