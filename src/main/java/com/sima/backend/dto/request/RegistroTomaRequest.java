package com.sima.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para confirmar u omitir una toma de medicamento.
 * HU-01: Confirmar toma desde la app (adulto mayor).
 * HU-02: Confirmar toma vía chatbot.
 * HU-13: Registrar manualmente toma (cuidador).
 * HU-13: Omitir toma manualmente (cuidador).
 */
@Getter
@Setter
@NoArgsConstructor
public class RegistroTomaRequest {

    @NotNull(message = "El ID del registro de toma es obligatorio")
    private Integer idRegistro;

    @NotBlank(message = "El método de confirmación es obligatorio")
    @Pattern(regexp = "^(app|chatbot|iot_pastillero|manual_cuidador|omision_cuidador)$",
             message = "El método debe ser: app, chatbot, iot_pastillero, manual_cuidador u omision_cuidador")
    private String metodoConfirmacion;

    // Nota opcional del cuidador al registrar la toma
    private String observacion;
}