package com.sima.backend.dto.response;

import com.sima.backend.entity.ObservacionCuidador;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ObservacionResponse {

    private Integer idObservacion;
    private Integer idAdulto;
    private Integer idCuidador;
    private String cuidadorNombre;
    private String urgencia;
    private String texto;
    private String tensionArterial;
    private String frecuenciaCardiaca;
    private String temperatura;
    private LocalDateTime fechaHora;

    public static ObservacionResponse from(ObservacionCuidador o) {
        ObservacionResponse dto = new ObservacionResponse();
        dto.setIdObservacion(o.getIdObservacion());
        dto.setIdAdulto(o.getAdulto().getIdAdulto());
        dto.setIdCuidador(o.getCuidador().getIdUsuario());
        dto.setCuidadorNombre(o.getCuidador().getNombre() + " " + o.getCuidador().getApellido());
        dto.setUrgencia(o.getUrgencia());
        dto.setTexto(o.getTexto());
        dto.setTensionArterial(o.getTensionArterial());
        dto.setFrecuenciaCardiaca(o.getFrecuenciaCardiaca());
        dto.setTemperatura(o.getTemperatura());
        dto.setFechaHora(o.getFechaHora());
        return dto;
    }
}
