package com.sima.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de respuesta al hacer login exitoso.
 * Incluye el token JWT y los datos básicos del usuario autenticado.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tipo; // Siempre "Bearer"
    private Integer idUsuario;
    private String nombre;
    private String apellido;
    private String correo;
    private String rol; // "Administrador" | "Familiar" | "Cuidador" | "Adulto Mayor"
}