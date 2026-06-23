package com.sima.backend.dto.response;

import com.sima.backend.entity.AdultoMayor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * DTO de respuesta para datos de un adulto mayor.
 * Incluye edad calculada para conveniencia del frontend.
 */
@Getter
@Setter
@NoArgsConstructor
public class AdultoMayorResponse {

    private Integer idAdulto;
    private String nombre;
    private String apellido;
    private LocalDate fechaNacimiento;
    private Integer edad; // Calculada al momento de la consulta
    private String condicionesMedicas;
    private String contactoMedico;
    private Boolean activo;
    private LocalDateTime creadoEn;

    public static AdultoMayorResponse from(AdultoMayor a) {
        AdultoMayorResponse dto = new AdultoMayorResponse();
        dto.setIdAdulto(a.getIdAdulto());
        dto.setNombre(a.getNombre());
        dto.setApellido(a.getApellido());
        dto.setFechaNacimiento(a.getFechaNacimiento());
        dto.setCondicionesMedicas(a.getCondicionesMedicas());
        dto.setContactoMedico(a.getContactoMedico());
        dto.setActivo(a.getActivo());
        dto.setCreadoEn(a.getCreadoEn());

        // Calcular edad si tiene fecha de nacimiento
        if (a.getFechaNacimiento() != null) {
            dto.setEdad(Period.between(a.getFechaNacimiento(), LocalDate.now()).getYears());
        }

        return dto;
    }
}