package com.sima.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para editar datos de una cuenta de usuario.
 * HU-17: Como Administrador, editar cuentas de familiares y cuidadores.
 * La contraseña y el rol son opcionales: si vienen null no se modifican.
 */
@Getter
@Setter
@NoArgsConstructor
public class UsuarioUpdateRequest {

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

    // Opcional: solo si el admin quiere cambiar la contraseña
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "La contraseña debe tener al menos una mayúscula, una minúscula y un número")
    private String password;

    // Opcional: solo si el admin quiere cambiar el rol
    private Integer idRol;

    // Opcional: para actualizar el OpenID de WeChat
    @Size(max = 100, message = "El OpenID de WeChat no puede superar 100 caracteres")
    private String wechatOpenid;
}