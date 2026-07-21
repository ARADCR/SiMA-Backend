package com.sima.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ActualizarDatosContactoCuidadorRequest {

    @NotBlank
    @Email
    @Size(max = 150)
    private String correo;

    @Size(max = 30)
    private String telefono;

    @Size(max = 100)
    private String ciudad;

    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal tarifaHora;

    @Size(max = 2000)
    private String disponibilidad;
}
