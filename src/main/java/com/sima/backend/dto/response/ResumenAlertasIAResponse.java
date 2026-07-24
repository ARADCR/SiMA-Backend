package com.sima.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ResumenAlertasIAResponse {

    @JsonProperty("resumenEjecutivo")
    private String resumenEjecutivo;

    @JsonProperty("alertasCriticas")
    private List<AlertaResumenDTO> alertasCriticas;

    @JsonProperty("alertasInformativas")
    private Integer alertasInformativas;

    @JsonProperty("alertasResueltas")
    private Integer alertasResueltas;

    @JsonProperty("escaladas")
    private List<EscaladaDTO> escaladas;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AlertaResumenDTO {
        @JsonProperty("idAlerta")
        private Integer idAlerta;

        @JsonProperty("tipo")
        private String tipo;

        @JsonProperty("mensaje")
        private String mensaje;

        @JsonProperty("justificacion")
        private String justificacion;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EscaladaDTO {
        @JsonProperty("descripcion")
        private String descripcion;

        @JsonProperty("alertasRelacionadas")
        private List<Integer> alertasRelacionadas;

        @JsonProperty("recomendacion")
        private String recomendacion;
    }
}
