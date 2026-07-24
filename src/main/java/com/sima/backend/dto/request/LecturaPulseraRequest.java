package com.sima.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de entrada para lecturas de pulsera inteligente.
 * El identificador físico (MAC) identifica el dispositivo;
 * los campos biométricos son opcionales porque la pulsera
 * envía diferentes paquetes en momentos distintos.
 */
@Getter
@Setter
@NoArgsConstructor
public class LecturaPulseraRequest {

    @NotBlank(message = "El identificador físico es obligatorio")
    private String identificadorFisico;

    @Min(value = 20, message = "La frecuencia cardíaca mínima es 20")
    @Max(value = 250, message = "La frecuencia cardíaca máxima es 250")
    private Integer frecuenciaCardiaca;

    @Min(value = 0, message = "El SpO2 mínimo es 0")
    @Max(value = 100, message = "El SpO2 máximo es 100")
    private Integer spo2;

    @Min(value = 40, message = "La presión sistólica mínima es 40")
    @Max(value = 300, message = "La presión sistólica máxima es 300")
    private Integer presionSistolica;

    @Min(value = 20, message = "La presión diastólica mínima es 20")
    @Max(value = 200, message = "La presión diastólica máxima es 200")
    private Integer presionDiastolica;

    @Min(value = 0, message = "Los pasos diarios no pueden ser negativos")
    private Integer pasosDiarios;

    @Min(value = 0, message = "El nivel de batería mínimo es 0")
    @Max(value = 100, message = "El nivel de batería máximo es 100")
    private Integer nivelBateria;

    @NotNull(message = "La fecha de medición es obligatoria")
    private LocalDateTime fechaMedicion;
}
