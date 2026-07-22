package com.sima.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReporteCreateRequest {
    @NotBlank(message = "El nombre del reporte es obligatorio")
    private String nombre;

    @NotBlank(message = "El tipo de reporte es obligatorio")
    private String tipo; // Semanal, Mensual, Trimestral, Personalizado

    @NotBlank(message = "El tipo de contenido del reporte es obligatorio")
    private String tipoReporte; // General, Medicacion, Alertas

    private Integer adultoMayorId;
    private String fechaInicio; // Formato ISO-8601 o yyyy-MM-dd
    private String fechaFin;   // Formato ISO-8601 o yyyy-MM-dd
}
