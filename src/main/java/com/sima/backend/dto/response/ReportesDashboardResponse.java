package com.sima.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportesDashboardResponse {
    private int cumplimientoPromedio;
    private long tomasRegistradasSemana;
    private long alertasGeneradas;
    private long alertasActivas;
    private long cuidadoresActivos;
    
    private List<ReporteDto> historial;
}
