package com.sima.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ResumenObservacionesResponse {

    @JsonProperty("resumen")
    private String resumen;

    @JsonProperty("observacionesAnalizadas")
    private Integer observacionesAnalizadas;

    @JsonProperty("periodoAnalizado")
    private String periodoAnalizado;

    @JsonProperty("alertasIdentificadas")
    private List<String> alertasIdentificadas;
}
