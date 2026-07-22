package com.sima.backend.dto.response;

import com.sima.backend.entity.PerfilCuidador;
import com.sima.backend.entity.Usuario;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DatosContactoCuidadorResponse {

    private String nombre;
    private String apellido;
    private String correo;
    private String telefono;
    private String ciudad;
    private BigDecimal tarifaHora;
    private String disponibilidad;

    public static DatosContactoCuidadorResponse from(Usuario usuario, PerfilCuidador perfil) {
        DatosContactoCuidadorResponse response = new DatosContactoCuidadorResponse();
        response.setNombre(usuario.getNombre());
        response.setApellido(usuario.getApellido());
        response.setCorreo(usuario.getCorreo());

        if (perfil != null) {
            response.setTelefono(perfil.getTelefono());
            response.setCiudad(perfil.getCiudad());
            response.setTarifaHora(perfil.getTarifaHora());
            response.setDisponibilidad(perfil.getDisponibilidad());
        }

        return response;
    }
}
