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
    private HorarioDto horario;
    private String estado; // pendiente | tomado | omitido | confirmado_manual
    private String metodoConfirmacion;
    private LocalDateTime fechaHoraProgramada;
    private LocalDateTime fechaHoraRegistro;
    private String observacion;
    private String nombreConfirmador; // Nombre del usuario que confirmó (null si fue IoT/sistema)

    @Getter
    @Setter
    @NoArgsConstructor
    public static class HorarioDto {
        private Integer idHorario;
        private String horaProgramada;
        private MedicamentoDto medicamento;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MedicamentoDto {
        private Integer idMedicamento;
        private String nombre;
        private String dosis;
    }

    public static RegistroTomaResponse from(RegistroToma rt) {
        RegistroTomaResponse dto = new RegistroTomaResponse();
        dto.setIdRegistro(rt.getIdRegistro());
        dto.setIdAdulto(rt.getAdulto().getIdAdulto());
        dto.setEstado(rt.getEstado());
        dto.setMetodoConfirmacion(rt.getMetodoConfirmacion());
        dto.setFechaHoraProgramada(rt.getFechaHoraProgramada());
        dto.setFechaHoraRegistro(rt.getFechaHoraRegistro());
        dto.setObservacion(rt.getObservacion());

        if (rt.getHorario() != null) {
            HorarioDto hDto = new HorarioDto();
            hDto.setIdHorario(rt.getHorario().getIdHorario());
            if (rt.getHorario().getHoraProgramada() != null) {
                hDto.setHoraProgramada(rt.getHorario().getHoraProgramada().toString());
            }

            if (rt.getHorario().getMedicamento() != null) {
                MedicamentoDto mDto = new MedicamentoDto();
                mDto.setIdMedicamento(rt.getHorario().getMedicamento().getIdMedicamento());
                mDto.setNombre(rt.getHorario().getMedicamento().getNombre());
                mDto.setDosis(rt.getHorario().getMedicamento().getDosis());
                hDto.setMedicamento(mDto);
            }
            dto.setHorario(hDto);
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