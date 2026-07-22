package com.sima.backend.dto.response;

import com.sima.backend.entity.Resena;
import lombok.Getter;
import lombok.Setter;

import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class ResenaResponse {
    private Integer id;
    private String familia;
    private String initials;
    private Integer puntos;
    private String texto;
    private String fecha;

    public static ResenaResponse from(Resena resena) {
        ResenaResponse response = new ResenaResponse();
        response.setId(resena.getIdResena());
        response.setFamilia("Familia " + resena.getFamiliar().getApellido());
        
        String nombreFamiliar = resena.getFamiliar().getNombre().trim();
        String apellidoFamiliar = resena.getFamiliar().getApellido().trim();
        response.setInitials(
            (nombreFamiliar.isEmpty() ? "" : nombreFamiliar.substring(0, 1)) +
            (apellidoFamiliar.isEmpty() ? "" : apellidoFamiliar.substring(0, 1))
        );

        response.setPuntos(resena.getPuntos());
        response.setTexto(resena.getTexto());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        response.setFecha(resena.getFechaCreacion().format(formatter));
        
        return response;
    }
}
