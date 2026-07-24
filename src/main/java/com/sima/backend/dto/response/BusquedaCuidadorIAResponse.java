package com.sima.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BusquedaCuidadorIAResponse {

    private String resumenBusqueda;
    private List<CuidadorRankeadoDTO> cuidadoresRankeados;

    @Getter
    @Setter
    public static class CuidadorRankeadoDTO {
        private Integer idUsuario;
        private String nombre;
        private Integer scoreRelevancia;
        private String justificacion;
    }
}
