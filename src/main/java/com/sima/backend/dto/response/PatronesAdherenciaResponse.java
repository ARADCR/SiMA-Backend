package com.sima.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PatronesAdherenciaResponse {

    @JsonProperty("patronesDetectados")
    private List<PatronDTO> patronesDetectados;

    // Mensaje informativo cuando no hay suficiente historial (< 7 días) o el LLM no pudo procesar la respuesta.
    // Null cuando se detectaron patrones normalmente.
    @JsonProperty("mensajeInformativo")
    private String mensajeInformativo;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PatronDTO {

        // Valores: "temporal" | "medicamento" | "metodo" | "tendencia"
        @JsonProperty("tipo")
        private String tipo;

        @JsonProperty("descripcion")
        private String descripcion;

        // Valores: "baja" | "media" | "alta"
        @JsonProperty("severidad")
        private String severidad;

        @JsonProperty("recomendacion")
        private String recomendacion;
    }
}
