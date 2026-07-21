package com.sima.backend.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EvaluarUrgenciaRequest {

    @NotNull(message = "El id del adulto mayor es obligatorio")
    private Integer idAdulto;

    private String tensionArterial;

    private String frecuenciaCardiaca;

    private String temperatura;

    private String textoObservacion;

    @AssertTrue(message = "Debe incluir al menos un signo vital (tensión arterial, frecuencia cardíaca o temperatura)")
    public boolean isTieneAlMenosUnSignoVital() {
        return (tensionArterial != null && !tensionArterial.isBlank())
                || (frecuenciaCardiaca != null && !frecuenciaCardiaca.isBlank())
                || (temperatura != null && !temperatura.isBlank());
    }
}
