package com.sima.backend.dto.response;

import com.sima.backend.entity.LecturaPulsera;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para lecturas de pulsera inteligente.
 */
@Getter
@Setter
@NoArgsConstructor
public class LecturaPulseraResponse {

    private Long idLectura;
    private Integer idDispositivo;
    private String identificadorFisico;
    private Integer idAdulto;
    private String nombreAdulto;
    private Integer frecuenciaCardiaca;
    private Integer spo2;
    private Integer presionSistolica;
    private Integer presionDiastolica;
    private Integer pasosDiarios;
    private Integer nivelBateria;
    private LocalDateTime fechaMedicion;
    private LocalDateTime fechaRecepcion;
    private String mensaje;

    /**
     * Convierte una entidad LecturaPulsera a su DTO de respuesta.
     */
    public static LecturaPulseraResponse from(LecturaPulsera l) {
        LecturaPulseraResponse dto = new LecturaPulseraResponse();
        dto.setIdLectura(l.getIdLectura());
        dto.setFrecuenciaCardiaca(l.getFrecuenciaCardiaca());
        dto.setSpo2(l.getSpo2());
        dto.setPresionSistolica(l.getPresionSistolica());
        dto.setPresionDiastolica(l.getPresionDiastolica());
        dto.setPasosDiarios(l.getPasosDiarios());
        dto.setNivelBateria(l.getNivelBateria());
        dto.setFechaMedicion(l.getFechaMedicion());
        dto.setFechaRecepcion(l.getFechaRecepcion());

        if (l.getDispositivo() != null) {
            dto.setIdDispositivo(l.getDispositivo().getIdDispositivo());
            dto.setIdentificadorFisico(l.getDispositivo().getIdentificadorFisico());
        }

        if (l.getAdulto() != null) {
            dto.setIdAdulto(l.getAdulto().getIdAdulto());
            dto.setNombreAdulto(l.getAdulto().getNombre() + " " + l.getAdulto().getApellido());
        }

        return dto;
    }

    /**
     * Convierte una entidad y agrega un mensaje personalizado.
     */
    public static LecturaPulseraResponse from(LecturaPulsera l, String mensaje) {
        LecturaPulseraResponse dto = from(l);
        dto.setMensaje(mensaje);
        return dto;
    }
}
