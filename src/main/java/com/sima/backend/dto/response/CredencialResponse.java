package com.sima.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CredencialResponse {
    private Integer id;
    private String tipo;
    private String nombre;
    private String fecha;
    private String estado;
    private String archivoUrl;
}
