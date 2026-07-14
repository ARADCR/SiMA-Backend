package com.sima.backend.dto.response;

import com.sima.backend.entity.Usuario;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CuidadorPublicResponse {
    private Integer idUsuario;
    private String nombre;
    private String apellido;
    private String especialidad; // Asumiremos un campo dummy por ahora o un valor fijo
    private Integer calificacion; // Dummy por ahora
    private String experiencia; // Dummy por ahora
    private String precio; // Dummy por ahora

    public static CuidadorPublicResponse from(Usuario usuario) {
        return CuidadorPublicResponse.builder()
                .idUsuario(usuario.getIdUsuario())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .especialidad("Cuidados Generales y Acompañamiento")
                .calificacion(5)
                .experiencia("3 años de experiencia")
                .precio("$15/hora")
                .build();
    }
}
