package com.sima.backend.dto.response;

import com.sima.backend.entity.RegistroToma;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para un registro de toma de medicamento.
 * Usado en el dashboard del familiar, cuidador y chatbot.
 */
@Getter
@Setter
@NoArgsConstructor
public class RegistroTomaResponse {

    private Integer idRegistro;
    private Integer idAdulto;
    private Integer idHorario;
    private String nombreMedicamento;
    private String dosis;
    private String estado; // pendiente | tomado | omitido | confirmado_manual
    private String metodoConfirmacion;
    private LocalDateTime fechaHoraProgramada;
    private LocalDateTime fechaHoraRegistro;
    private String observacion;
    private String nombreConfirmador; // Nombre del usuario que confirmó (null si fue IoT/sistema)

    public static RegistroTomaResponse from(RegistroToma rt) {
        RegistroTomaResponse dto = new RegistroTomaResponse();
        dto.setIdRegistro(rt.getIdRegistro());
        dto.setIdAdulto(rt.getAdulto().getIdAdulto());
        dto.setIdHorario(rt.getHorario().getIdHorario());
        dto.setEstado(rt.getEstado());
        dto.setMetodoConfirmacion(rt.getMetodoConfirmacion());
        dto.setFechaHoraProgramada(rt.getFechaHoraProgramada());
        dto.setFechaHoraRegistro(rt.getFechaHoraRegistro());
        dto.setObservacion(rt.getObservacion());

        // Datos del medicamento desde el horario
        if (rt.getHorario() != null && rt.getHorario().getMedicamento() != null) {
            dto.setNombreMedicamento(rt.getHorario().getMedicamento().getNombre());
            dto.setDosis(rt.getHorario().getMedicamento().getDosis());
        }

        // Nombre del confirmador si aplica
        if (rt.getUsuarioConfirmador() != null) {
            dto.setNombreConfirmador(
                    rt.getUsuarioConfirmador().getNombre() + " " +
                            rt.getUsuarioConfirmador().getApellido());
        }

        return dto;
    }
}