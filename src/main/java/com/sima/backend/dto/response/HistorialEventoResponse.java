package com.sima.backend.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
public class HistorialEventoResponse {
    private Integer id;
    private String tipo;      // "toma" | "alerta" | "actividad_iot"
    private String subtipo;   // estado de toma, tipo de alerta o evento IoT
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaHora;
    private Map<String, Object> meta;
}
