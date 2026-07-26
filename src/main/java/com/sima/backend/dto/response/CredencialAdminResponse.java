package com.sima.backend.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CredencialAdminResponse {
    private Integer id;
    private String cuidador;
    private String initials;
    private String avatarBg;
    private String tipo;
    private String documento;
    private String fecha;
    private String estado;
    private String archivoUrl;
}
