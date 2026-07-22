package com.sima.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CuidadorStatsResponse {
    private int pacientes;
    private double calificacion;
    private long tomasRegistradas;
    private int cumplimiento;
}
