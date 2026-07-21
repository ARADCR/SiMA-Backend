package com.sima.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AnalisisPerfilResponse {

    private List<String> especialidadesDetectadas;
    private String experienciaEstimada;
    private List<String> certificacionesDetectadas;
    private String resumenGenerado;
    private List<String> advertencias;
    private List<String> tagsRecomendados;
}
