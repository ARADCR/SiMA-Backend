package com.sima.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MatchCuidadorResponse {

    private Integer scoreCompatibilidad;
    private String justificacion;
    private List<String> areasFortaleza;
    private List<String> areasAtencion;
}
