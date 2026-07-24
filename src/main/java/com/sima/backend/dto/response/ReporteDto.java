package com.sima.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteDto {
    private Integer id;
    private String nombre;
    private String tipo;
    private String fecha;
    private String generadoPor;
    private String estado;
}
