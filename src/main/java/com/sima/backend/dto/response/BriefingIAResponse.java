package com.sima.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * HU-25: Briefing diario inteligente del dashboard, adaptado al rol del usuario
 * (Cuidador o Familiar).
 */
@Getter
@Setter
@NoArgsConstructor
public class BriefingIAResponse {

    @JsonProperty("saludo")
    private String saludo;

    @JsonProperty("resumenGeneral")
    private String resumenGeneral;

    @JsonProperty("pacientes")
    private List<PacienteBriefingDTO> pacientes;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PacienteBriefingDTO {
        @JsonProperty("idAdulto")
        private Integer idAdulto;

        @JsonProperty("nombre")
        private String nombre;

        @JsonProperty("prioridad")
        private String prioridad; // "alta" | "media" | "baja"

        @JsonProperty("resumen")
        private String resumen;

        @JsonProperty("proximaToma")
        private String proximaToma;

        @JsonProperty("adherenciaSemana")
        private Double adherenciaSemana;
    }
}
