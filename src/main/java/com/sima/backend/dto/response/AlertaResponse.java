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

    private Integer id;
    private Integer adultoMayorId;
    private String nombreAdulto;
    private String tipo; // omision_medicacion | caida_detectada | emergencia | bateria_baja
    private String descripcion;
    private String estado;
    private String prioridad;
    private LocalDateTime timestamp;

    // Origen de la alerta (solo uno de los dos vendrá con valor)
    private Integer idRegistro; // Si viene de toma omitida
    private Integer idEventoIot; // Si viene de evento IoT

    public static AlertaResponse from(Alerta a) {
        AlertaResponse dto = new AlertaResponse();
        dto.setId(a.getIdAlerta());
        dto.setAdultoMayorId(a.getAdulto().getIdAdulto());
        dto.setNombreAdulto(a.getAdulto().getNombre() + " " + a.getAdulto().getApellido());
        dto.setTipo(a.getTipoAlerta());
        dto.setDescripcion(a.getMensaje());
        dto.setEstado(a.getResuelta() ? "resuelta" : "pendiente");
        
        // Asignar prioridad basada en el tipo de alerta
        if ("emergencia".equals(a.getTipoAlerta()) || "caida_detectada".equals(a.getTipoAlerta())) {
            dto.setPrioridad("critica");
        } else {
            dto.setPrioridad("moderada");
        }

        dto.setTimestamp(a.getCreadoEn());

        if (a.getRegistro() != null) {
            dto.setIdRegistro(a.getRegistro().getIdRegistro());
        }
        if (a.getEventoIot() != null) {
            dto.setIdEventoIot(a.getEventoIot().getIdEvento());
        }

        return dto;
    }
}