package com.sima.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReporteMedicionSemanalResponse {

    private double porcentajeAdherencia;
    private int totalProgramadas;
    private int totalTomadas;
    private int totalOmitidas;
    private List<DetalleDiario> desgloseDiario;
    private List<MedicamentoOmitido> medicamentosMasOmitidos;

    @Getter
    @Builder
    public static class DetalleDiario {
        private String fecha;           // ISO date: "2026-07-07"
        private int totalProgramadas;
        private int totalTomadas;
    }

    @Getter
    @Builder
    public static class MedicamentoOmitido {
        private String nombre;
        private int cantidadOmisiones;
    }
}
