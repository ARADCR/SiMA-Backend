package com.sima.backend.dto.response;

import com.sima.backend.entity.Alerta;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para alertas del sistema.
 * Usado en el dashboard del familiar, cuidador y administrador.
 */
@Getter
@Setter
@NoArgsConstructor
public class AlertaResponse {

    private Integer idAlerta;
    private Integer idAdulto;
    private String nombreAdulto;
    private String tipoAlerta; // omision_medicacion | caida_detectada | emergencia | bateria_baja
    private String mensaje;
    private Boolean resuelta;
    private LocalDateTime creadoEn;

    // Origen de la alerta (solo uno de los dos vendrá con valor)
    private Integer idRegistro; // Si viene de toma omitida
    private Integer idEventoIot; // Si viene de evento IoT

    public static AlertaResponse from(Alerta a) {
        AlertaResponse dto = new AlertaResponse();
        dto.setIdAlerta(a.getIdAlerta());
        dto.setIdAdulto(a.getAdulto().getIdAdulto());
        dto.setNombreAdulto(a.getAdulto().getNombre() + " " + a.getAdulto().getApellido());
        dto.setTipoAlerta(a.getTipoAlerta());
        dto.setMensaje(a.getMensaje());
        dto.setResuelta(a.getResuelta());
        dto.setCreadoEn(a.getCreadoEn());

        if (a.getRegistro() != null) {
            dto.setIdRegistro(a.getRegistro().getIdRegistro());
        }
        if (a.getEventoIot() != null) {
            dto.setIdEventoIot(a.getEventoIot().getIdEvento());
        }

        return dto;
    }
}