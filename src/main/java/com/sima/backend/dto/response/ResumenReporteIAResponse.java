package com.sima.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ResumenReporteIAResponse {

    @JsonProperty("resumenNarrativo")
    private String resumenNarrativo;

    @JsonProperty("puntosClave")
    private List<String> puntosClave;

    @JsonProperty("recomendaciones")
    private List<String> recomendaciones;
}
