package com.sima.backend.dto.response;

import com.sima.backend.entity.HorarioMedicamento;
import com.sima.backend.entity.Medicamento;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO de respuesta para un medicamento con sus horarios activos.
 */
@Getter
@Setter
@NoArgsConstructor
public class MedicamentoResponse {

    private Integer idMedicamento;
    private Integer idAdulto;
    private String nombre;
    private String dosis;
    private Integer frecuenciaHoras;
    private Boolean activo;
    private String observaciones;
    private String principioActivo;
    private LocalDate fechaFin;
    private Integer stockActual;
    private Integer stockMinimo;
    private String prescritoPor;
    private LocalDateTime creadoEn;
    private List<HorarioResponse> horarios;

    public static MedicamentoResponse from(Medicamento m) {
        MedicamentoResponse dto = new MedicamentoResponse();
        dto.setIdMedicamento(m.getIdMedicamento());
        dto.setIdAdulto(m.getAdulto().getIdAdulto());
        dto.setNombre(m.getNombre());
        dto.setDosis(m.getDosis());
        dto.setFrecuenciaHoras(m.getFrecuenciaHoras());
        dto.setActivo(m.getActivo());
        dto.setObservaciones(m.getObservaciones());
        dto.setPrincipioActivo(m.getPrincipioActivo());
        dto.setFechaFin(m.getFechaFin());
        dto.setStockActual(m.getStockActual());
        dto.setStockMinimo(m.getStockMinimo());
        dto.setPrescritoPor(m.getPrescritoPor());
        dto.setCreadoEn(m.getCreadoEn());

        if (m.getHorarios() != null) {
            dto.setHorarios(
                    m.getHorarios().stream()
                            .filter(HorarioMedicamento::getActivo)
                            .map(HorarioResponse::from)
                            .toList());
        }

        return dto;
    }

    // Clase interna para los horarios del medicamento
    @Getter
    @Setter
    @NoArgsConstructor
    public static class HorarioResponse {
        private Integer idHorario;
        private LocalTime horaProgramada;
        private Boolean activo;

        public static HorarioResponse from(HorarioMedicamento h) {
            HorarioResponse dto = new HorarioResponse();
            dto.setIdHorario(h.getIdHorario());
            dto.setHoraProgramada(h.getHoraProgramada());
            dto.setActivo(h.getActivo());
            return dto;
        }
    }
}