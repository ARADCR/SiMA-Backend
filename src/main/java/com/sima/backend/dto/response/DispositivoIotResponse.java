package com.sima.backend.dto.response;

import com.sima.backend.entity.DispositivoIot;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para datos de un dispositivo IoT.
 */
@Getter
@Setter
@NoArgsConstructor
public class DispositivoIotResponse {

    private Integer idDispositivo;
    private String identificadorFisico;
    private String tipoDispositivo;
    private Integer idAdulto;
    private String nombreAdulto; // Nombre completo del adulto asignado (null si sin asignar)
    private Boolean activo;
    private LocalDateTime fechaRegistro;

    public static DispositivoIotResponse from(DispositivoIot d) {
        DispositivoIotResponse dto = new DispositivoIotResponse();
        dto.setIdDispositivo(d.getIdDispositivo());
        dto.setIdentificadorFisico(d.getIdentificadorFisico());
        dto.setTipoDispositivo(d.getTipoDispositivo());
        dto.setActivo(d.getActivo());
        dto.setFechaRegistro(d.getFechaRegistro());

        if (d.getAdulto() != null) {
            dto.setIdAdulto(d.getAdulto().getIdAdulto());
            dto.setNombreAdulto(d.getAdulto().getNombre() + " " + d.getAdulto().getApellido());
        }

        return dto;
    }
}