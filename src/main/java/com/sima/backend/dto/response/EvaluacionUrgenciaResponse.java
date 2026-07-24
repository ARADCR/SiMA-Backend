package com.sima.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class EvaluacionUrgenciaResponse {

    @JsonProperty("urgenciaSugerida")
    private String urgenciaSugerida;

    @JsonProperty("justificacion")
    private String justificacion;

    @JsonProperty("valoresAnormales")
    private List<String> valoresAnormales;

    @JsonProperty("recomendaciones")
    private List<String> recomendaciones;
}
