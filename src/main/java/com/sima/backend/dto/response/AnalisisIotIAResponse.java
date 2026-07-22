package com.sima.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class AnalisisIotIAResponse {

    @JsonProperty("resumenEstado")
    private String resumenEstado;

    @JsonProperty("anomaliasDetectadas")
    private List<AnomaliaIotDTO> anomaliasDetectadas;

    @JsonProperty("tendencias")
    private List<TendenciaDTO> tendencias;

    @JsonProperty("fechaAnalisis")
    private LocalDateTime fechaAnalisis;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AnomaliaIotDTO {
        @JsonProperty("tipo")
        private String tipo; // vital | actividad | pastillero | correlacion

        @JsonProperty("descripcion")
        private String descripcion;

        @JsonProperty("severidad")
        private String severidad; // baja | media | alta | critica

        @JsonProperty("datosRelevantes")
        private Map<String, String> datosRelevantes;

        @JsonProperty("recomendacion")
        private String recomendacion;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TendenciaDTO {
        @JsonProperty("descripcion")
        private String descripcion;

        @JsonProperty("direccion")
        private String direccion; // subiendo | bajando | estable

        @JsonProperty("periodo")
        private String periodo;
    }
}
