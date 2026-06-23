package com.sima.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para crear una cuenta de usuario.
 * HU-17: Como Administrador, crear cuentas de familiares y cuidadores.
 */
@Getter
@Setter
@NoArgsConstructor
public class UsuarioCreateRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar 100 caracteres")
    private String apellido;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 120, message = "El correo no puede superar 120 caracteres")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "La contraseña debe tener al menos una mayúscula, una minúscula y un número")
    private String password;

    @NotNull(message = "El ID de rol es obligatorio")
    private Integer idRol;

    // Opcional: solo para usuarios que van a recibir notificaciones WeChat
    @Size(max = 100, message = "El OpenID de WeChat no puede superar 100 caracteres")
    private String wechatOpenid;
}