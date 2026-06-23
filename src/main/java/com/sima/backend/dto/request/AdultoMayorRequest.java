package com.sima.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO para crear o actualizar datos de un adulto mayor.
 * HU-12: Como Familiar, registrar o actualizar datos del adulto mayor.
 */
@Getter
@Setter
@NoArgsConstructor
public class AdultoMayorRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar 100 caracteres")
    private String apellido;

    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate fechaNacimiento;

    private String condicionesMedicas;

    private String contactoMedico;
}