package com.sima.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para registrar un dispositivo IoT.
 * HU-18: Como Administrador, registrar y asignar dispositivos IoT a un adulto
 * mayor.
 */
@Getter
@Setter
@NoArgsConstructor
public class DispositivoIotRequest {

    @NotBlank(message = "El identificador físico es obligatorio")
    @Size(max = 80, message = "El identificador no puede superar 80 caracteres")
    private String identificadorFisico; // MAC address o código físico único

    @NotBlank(message = "El tipo de dispositivo es obligatorio")
    @Pattern(regexp = "^(pastillero_esp32|pulsera_inteligente)$", message = "El tipo debe ser 'pastillero_esp32' o 'pulsera_inteligente'")
    private String tipoDispositivo;

    // Opcional al registrar: se puede asignar después
    private Integer idAdulto;
}