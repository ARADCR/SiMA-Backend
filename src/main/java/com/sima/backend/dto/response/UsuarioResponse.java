package com.sima.backend.dto.response;

import com.sima.backend.entity.Usuario;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para datos de un usuario.
 * Nunca expone password_hash ni datos sensibles.
 */
@Getter
@Setter
@NoArgsConstructor
public class UsuarioResponse {

    private Integer idUsuario;
    private String nombre;
    private String apellido;
    private String correo;
    private Integer idRol;
    private String nombreRol;
    private String wechatOpenid;
    private Boolean activo;
    private LocalDateTime creadoEn;
    private LocalDateTime ultimoAcceso;

    // Mapeo manual desde entidad (sin MapStruct por simplicidad académica)
    public static UsuarioResponse from(Usuario u) {
        UsuarioResponse dto = new UsuarioResponse();
        dto.setIdUsuario(u.getIdUsuario());
        dto.setNombre(u.getNombre());
        dto.setApellido(u.getApellido());
        dto.setCorreo(u.getCorreo());
        dto.setIdRol(u.getRol().getIdRol());
        dto.setNombreRol(u.getRol().getNombreRol());
        dto.setWechatOpenid(u.getWechatOpenid());
        dto.setActivo(u.getActivo());
        dto.setCreadoEn(u.getCreadoEn());
        dto.setUltimoAcceso(u.getUltimoAcceso());
        return dto;
    }
}