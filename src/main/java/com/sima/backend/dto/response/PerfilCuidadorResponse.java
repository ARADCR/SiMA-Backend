package com.sima.backend.dto.response;

import com.sima.backend.entity.PerfilCuidador;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class PerfilCuidadorResponse {

    private String descripcionPerfil;
    private List<String> especialidades;
    private String experiencia;
    private List<String> certificaciones;
    private String resumenIa;
    private List<String> tags;
    private boolean perfilAnalizado;

    public static PerfilCuidadorResponse from(PerfilCuidador perfil) {
        PerfilCuidadorResponse response = new PerfilCuidadorResponse();

        if (perfil == null || !Boolean.TRUE.equals(perfil.getPerfilAnalizado())) {
            response.setDescripcionPerfil(perfil != null ? perfil.getDescripcionPerfil() : null);
            response.setEspecialidades(Collections.emptyList());
            response.setExperiencia(null);
            response.setCertificaciones(Collections.emptyList());
            response.setResumenIa(null);
            response.setTags(Collections.emptyList());
            response.setPerfilAnalizado(false);
            return response;
        }

        response.setDescripcionPerfil(perfil.getDescripcionPerfil());
        response.setEspecialidades(dividir(perfil.getEspecialidades()));
        response.setExperiencia(perfil.getExperienciaAnios() != null
                ? perfil.getExperienciaAnios() + " años de experiencia"
                : null);
        response.setCertificaciones(dividir(perfil.getCertificaciones()));
        response.setResumenIa(perfil.getResumenIa());
        response.setTags(dividir(perfil.getTags()));
        response.setPerfilAnalizado(true);
        return response;
    }

    private static List<String> dividir(String valor) {
        if (valor == null || valor.isBlank()) return Collections.emptyList();
        return Arrays.stream(valor.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
