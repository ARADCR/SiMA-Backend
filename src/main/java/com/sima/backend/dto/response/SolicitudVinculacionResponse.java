package com.sima.backend.dto.response;

import com.sima.backend.entity.SolicitudVinculacion;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SolicitudVinculacionResponse {
    private Integer idSolicitud;
    private String nombreFamiliar;
    private String correoFamiliar;
    private String nombreAdulto;
    private Integer idAdulto;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaRespuesta;

    public static SolicitudVinculacionResponse from(SolicitudVinculacion entity) {
        return SolicitudVinculacionResponse.builder()
                .idSolicitud(entity.getIdSolicitud())
                .nombreFamiliar(entity.getFamiliar().getNombre() + " " + entity.getFamiliar().getApellido())
                .correoFamiliar(entity.getFamiliar().getCorreo())
                .nombreAdulto(entity.getAdulto().getNombre() + " " + entity.getAdulto().getApellido())
                .idAdulto(entity.getAdulto().getIdAdulto())
                .estado(entity.getEstado())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaRespuesta(entity.getFechaRespuesta())
                .build();
    }
}
