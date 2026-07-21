package com.sima.backend.dto.response;

import com.sima.backend.entity.PerfilCuidador;
import com.sima.backend.entity.Usuario;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CuidadorPublicResponse {
    private Integer idUsuario;
    private String nombre;
    private String apellido;
    private String especialidad;
    private Integer calificacion; // Dummy por ahora — sistema de reviews es feature separada
    private String experiencia;
    private String precio; // Dummy por ahora — no hay modelo de tarifas todavía
    private String resumenIa;

    public static CuidadorPublicResponse from(Usuario usuario) {
        return from(usuario, null);
    }

    public static CuidadorPublicResponse from(Usuario usuario, PerfilCuidador perfil) {
        boolean tienePerfilAnalizado = perfil != null && Boolean.TRUE.equals(perfil.getPerfilAnalizado());

        return CuidadorPublicResponse.builder()
                .idUsuario(usuario.getIdUsuario())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .especialidad(tienePerfilAnalizado && perfil.getEspecialidades() != null
                        ? perfil.getEspecialidades()
                        : "Cuidados Generales y Acompañamiento")
                .calificacion(5)
                .experiencia(tienePerfilAnalizado && perfil.getExperienciaAnios() != null
                        ? perfil.getExperienciaAnios() + " años de experiencia"
                        : "3 años de experiencia")
                .precio(perfil != null && perfil.getTarifaHora() != null
                        ? "$" + perfil.getTarifaHora() + "/hora"
                        : "$15/hora")
                .resumenIa(tienePerfilAnalizado ? perfil.getResumenIa() : null)
                .build();
    }
}
